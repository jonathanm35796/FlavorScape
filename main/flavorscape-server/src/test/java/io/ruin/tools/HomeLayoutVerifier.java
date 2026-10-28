package io.ruin.tools;

import com.google.gson.*;
import io.ruin.cache.ObjectDef;
import io.ruin.model.World;
import io.ruin.model.map.Tile;
import io.ruin.model.map.object.GameObject;
import io.ruin.model.map.Direction;
import java.nio.file.*;
import java.util.*;

/** Integration validation against actual cache clipping and the staged spawn data. */
public final class HomeLayoutVerifier {
    private static final int X=3040,Y=3430,W=121,H=90;
    private static final List<String> failures=new ArrayList<>();
    private static JsonArray read(Path file) throws Exception {return new JsonParser().parse(Files.readString(file)).getAsJsonArray();}
    private static int n(JsonObject o,String key){return o.has(key)?o.get(key).getAsInt():0;}
    private static boolean active(JsonObject o){return !o.has("world")||o.get("world").isJsonNull()||o.get("world").getAsString().equals("PVP");}
    private static boolean inside(int x,int y){return x>=X&&x<X+W&&y>=Y&&y<Y+H;}
    public static void main(String[] args){
        try {
            HomeMapAudit.initialize(args[0],args[1]);
            Path data=Path.of(args[1]),output=Path.of(args[2]);
            List<JsonObject> npcs=new ArrayList<>(),objects=new ArrayList<>();
            try(var paths=Files.list(data.resolve("objects/spawns"))){for(Path file:paths.filter(p->p.toString().endsWith(".json")).toList())for(JsonElement e:read(file)){
                JsonObject o=e.getAsJsonObject();if(!active(o)||n(o,"z")!=0||!inside(n(o,"x"),n(o,"y")))continue;
                if(o.has("remove")&&o.get("remove").getAsBoolean()) {
                    GameObject old=Tile.getObject(n(o,"id"),n(o,"x"),n(o,"y"),0,n(o,"type"),n(o,"direction"));
                    if(old!=null)old.remove();continue;
                }
                if(ObjectDef.get(n(o,"id"))==null){failures.add("Missing object definition "+o);continue;}
                GameObject.spawn(n(o,"id"),n(o,"x"),n(o,"y"),0,n(o,"type"),n(o,"direction"));objects.add(o);
            }}
            Map<String,Boolean> occupied=new HashMap<>();
            List<String> foreign=new ArrayList<>(List.of("id,x,y,z"));
            List<JsonObject> managed=new ArrayList<>();
            try(var paths=Files.list(data.resolve("npcs/spawns"))){for(Path file:paths.filter(p->p.toString().endsWith(".json")).toList())for(JsonElement e:read(file)){
                JsonObject o=e.getAsJsonObject();if(!active(o)||n(o,"z")!=0||!inside(n(o,"x"),n(o,"y")))continue;
                boolean homeFile=file.getFileName().toString().equals("edgeville.json")||file.getFileName().toString().equals("home.json");
                String coordinate=n(o,"x")+","+n(o,"y");
                if(occupied.containsKey(coordinate)&&(homeFile||occupied.get(coordinate)))failures.add("Duplicate home NPC tile "+coordinate);
                occupied.put(coordinate,homeFile||occupied.getOrDefault(coordinate,false));
                if(homeFile)managed.add(o);else foreign.add(n(o,"id")+","+n(o,"x")+","+n(o,"y")+",0");
                if(n(o,"walkRange")==0)Tile.get(n(o,"x"),n(o,"y"),0,true).flagUnmovable();npcs.add(o);
            }}
            boolean[][] reachable=reachable();
            if(Tile.getObject(50004,3097,3508,0,10,3)!=null)failures.add("Rejuvenation pool left at its old position");
            if(Tile.getObject(617,3097,3506,0,10,1)!=null)failures.add("Table left in front of the PvP shop merchant");
            for(int[] placement:new int[][]{{50004,3097,3510,3}}){
                GameObject object=Tile.getObject(placement[0],placement[1],placement[2],0,10,placement[3]);
                if(object==null){failures.add("Missing workshop object "+Arrays.toString(placement));continue;}
                ObjectDef def=object.getDef();int width=def.xLength,height=def.yLength;
                if((placement[3]&1)!=0){int swap=width;width=height;height=swap;}
                if(placement[1]<3091||placement[2]<3506||placement[1]+width-1>3098||placement[2]+height-1>3511)
                    failures.add("Workshop object extends outside the requested room: "+def.name);
            }
            for(int[] banker:new int[][]{{3096,3489},{3096,3491},{3096,3492},{3098,3492}})
                if(npcs.stream().noneMatch(o->n(o,"id")==3888&&n(o,"x")==banker[0]&&n(o,"y")==banker[1]))failures.add("Original bank NPC missing "+Arrays.toString(banker));
            if(npcs.stream().noneMatch(o->n(o,"id")==9001&&n(o,"x")==3098&&n(o,"y")==3506))failures.add("Market merchant missing from southeast room corner");
            if(npcs.stream().noneMatch(o->n(o,"id")==15000&&n(o,"x")==3099&&n(o,"y")==3510))failures.add("Skully missing from requested tile");
            if(Tile.getObject(50005,3095,3506,0,10,1)==null)failures.add("Loot chest missing from requested tile");
            if(!r(reachable,3096,3506)||!r(reachable,3097,3506))failures.add("Direct market merchant approach blocked");
            if(!r(reachable,World.HOME.getX(),World.HOME.getY()))failures.add("Arrival tile blocked");
            if(!r(reachable,3088,3518))failures.add("Northern wilderness approach disconnected");
            for(JsonObject npc:managed){int x=n(npc,"x"),y=n(npc,"y");boolean accessible=approach(reachable,x,y,1,1);
                if(!accessible&&(n(npc,"id")==3888||n(npc,"id")==9001)){Direction d=Direction.get(npc.has("direction")?npc.get("direction").getAsString():"S");GameObject counter=Tile.getObject(-1,x+d.deltaX,y+d.deltaY,0,10,-1);
                    accessible=counter!=null&&counter.getDef().xLength==1&&counter.getDef().yLength==1&&r(reachable,x+d.deltaX*2,y+d.deltaY*2);}
                if(!accessible)failures.add("NPC inaccessible "+npc.get("id")+" "+npc.get("name")+" "+x+","+y);}
            for(JsonObject object:objects){ObjectDef def=ObjectDef.get(n(object,"id"));int w=def.xLength,h=def.yLength;if(n(object,"direction")==1||n(object,"direction")==3){int swap=w;w=h;h=swap;}
                if(!approach(reachable,n(object,"x"),n(object,"y"),w,h))failures.add("Object inaccessible "+def.name+" "+n(object,"x")+","+n(object,"y"));}
            JsonObject manifest=new JsonParser().parse(Files.readString(data.resolve("home/edgeville-migration.json"))).getAsJsonObject();
            JsonArray configured=read(data.resolve("npcs/spawns/edgeville.json"));
            Set<String> destinations=new HashSet<>();
            for(JsonElement e:manifest.getAsJsonArray("npcs")){
                JsonObject row=e.getAsJsonObject(),origin=row.getAsJsonObject("origin"),dest=row.getAsJsonObject("destination");
                String key=n(dest,"id")+":"+n(dest,"x")+":"+n(dest,"y")+":"+n(dest,"z");
                if(!destinations.add(key))failures.add("Lost NPC instance through duplicate migration destination "+key);
                JsonObject actual=null;for(JsonElement candidate:configured){JsonObject p=candidate.getAsJsonObject();if(n(p,"id")==n(dest,"id")&&n(p,"x")==n(dest,"x")&&n(p,"y")==n(dest,"y")&&n(p,"z")==n(dest,"z")){actual=p;break;}}
                if(actual==null){failures.add("Missing migrated NPC "+key);continue;}
                if(origin.has("shopOptions")&&!origin.get("shopOptions").isJsonNull()&&!origin.get("shopOptions").equals(actual.get("shopOptions")))failures.add("Shop bindings changed for "+key);
                if(World.LEGACY_HOME_BOUNDS.inBounds(n(actual,"x"),n(actual,"y"),n(actual,"z"),0))failures.add("NPC left at retired home "+key);
            }
            JsonObject exit=manifest.getAsJsonObject("housingExit");if(!r(reachable,n(exit,"x"),n(exit,"y")))failures.add("Housing exit blocked "+exit);
            for(JsonElement e:manifest.getAsJsonArray("fishingPositions")){JsonObject p=e.getAsJsonObject();if(!approach(reachable,n(p,"x"),n(p,"y"),1,1))failures.add("Fishing spot inaccessible "+p);}
            Files.createDirectories(output);
            Files.write(output.resolve("foreign-npcs.csv"),foreign);
            List<String> rows=new ArrayList<>(List.of("x,y,z,clipping,reachable"));
            for(int x=X;x<X+W;x++)for(int y=Y;y<Y+H;y++){Tile tile=Tile.get(x,y,0);rows.add(x+","+y+",0,"+(tile==null?0:tile.clipping)+","+r(reachable,x,y));}
            Files.write(output.resolve("planned-tiles.csv"),rows);
            Files.write(output.resolve("validation.txt"),failures.isEmpty()?List.of("PASS: "+npcs.size()+" active NPCs, "+objects.size()+" dynamic objects and all fishing approaches reachable from Edgeville home."):failures);
            failures.forEach(System.err::println);
            System.out.println("HOME_LAYOUT_VERIFY "+(failures.isEmpty()?"PASS":"FAIL")+" NPCs="+npcs.size()+" objects="+objects.size()+" failures="+failures.size());
            System.exit(failures.isEmpty()?0:1);
        }catch(Throwable error){error.printStackTrace();System.exit(1);}
    }
    private static boolean[][] reachable(){
        boolean[][] visited=new boolean[W][H];ArrayDeque<Integer> q=new ArrayDeque<>();
        int sx=World.HOME.getX()-X,sy=World.HOME.getY()-Y;q.add(sx*H+sy);visited[sx][sy]=true;
        int[] dx={-1,1,0,0},dy={0,0,-1,1},mask={0x1240108,0x1240180,0x1240102,0x1240120};
        while(!q.isEmpty()){int p=q.remove(),x=p/H,y=p%H;for(int d=0;d<4;d++){int nx=x+dx[d],ny=y+dy[d];if(nx<0||ny<0||nx>=W||ny>=H||visited[nx][ny])continue;Tile tile=Tile.get(nx+X,ny+Y,0);if(tile!=null&&(tile.clipping&mask[d])!=0)continue;visited[nx][ny]=true;q.add(nx*H+ny);}}
        return visited;
    }
    private static boolean r(boolean[][] grid,int x,int y){return inside(x,y)&&grid[x-X][y-Y];}
    private static boolean approach(boolean[][] grid,int x,int y,int w,int h){
        for(int i=x;i<x+w;i++)if(r(grid,i,y-1)||r(grid,i,y+h))return true;
        for(int j=y;j<y+h;j++)if(r(grid,x-1,j)||r(grid,x+w,j))return true;
        return false;
    }
}
