package io.ruin.tools;

import com.google.gson.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/** Source/data coverage inventory. Presence and review markers are not gameplay certification. */
public final class ContentInventoryAudit {
    private static final Pattern REG=Pattern.compile("(?:ItemAction|ItemNPCAction|NPCAction|ObjectAction|InterfaceHandler|MapListener|LoginListener|SpawnListener)\\.\\w+\\s*\\(|when(?:Obj|Npc|Item|Region)\\w*\\s*\\(");
    private static int count(Pattern pattern,String value){int n=0;Matcher m=pattern.matcher(value);while(m.find())n++;return n;}
    private static String group(String path){
        for(String root:new String[]{"model/skills/","model/activities/","content/activities/","content/areas/","model/entity/npc/actions/","model/map/object/actions/","model/item/actions/","model/content/"}){
            int i=path.indexOf(root);if(i<0)continue;
            String tail=path.substring(i+root.length());int slash=tail.indexOf('/');
            return root+(slash<0?"(root)":tail.substring(0,slash));
        }
        int index=path.indexOf("io/ruin/");String tail=index<0?path:path.substring(index+8);
        String[] parts=tail.split("/");return String.join("/",Arrays.copyOf(parts,Math.min(2,parts.length)));
    }
    private static String uncomment(String source){
        StringBuilder result=new StringBuilder();int state=0;
        for(int i=0;i<source.length();i++){
            char c=source.charAt(i),next=i+1<source.length()?source.charAt(i+1):0;
            if(state==1){if(c=='\n'){state=0;result.append(c);}else result.append(' ');continue;}
            if(state==2){if(c=='*'&&next=='/'){state=0;result.append("  ");i++;}else result.append(c=='\n'?'\n':' ');continue;}
            if(state==3||state==4){result.append(c);if(c=='\\'&&next!=0){result.append(next);i++;}else if(c==(state==3?'"':'\''))state=0;continue;}
            if(c=='/'&&next=='/'){state=1;result.append("  ");i++;}
            else if(c=='/'&&next=='*'){state=2;result.append("  ");i++;}
            else {result.append(c);if(c=='"')state=3;else if(c=='\'')state=4;}
        }
        return result.toString();
    }
    public static void main(String[] args){
        try{run();}catch(Throwable failure){failure.printStackTrace();System.exit(1);}
    }
    private static void run()throws Exception{
        Path server=Path.of(".").toAbsolutePath().normalize(),out=Path.of("../../docs/content-audit");Files.createDirectories(out);
        List<String> sources=new ArrayList<>(List.of("component\tpath\tlanguage\tlines\tregistration_calls\tempty_method_markers\treturn_null_markers\ttodo_markers\tpvp_mentions\teco_mentions"));
        Map<String,int[]> summary=new TreeMap<>();
        Pattern empty=Pattern.compile("(?:public|protected|private|override)\\s+[^;{}\\n]*\\([^;{}]*\\)\\s*\\{\\s*\\}");
        Pattern nulls=Pattern.compile("return\\s+null\\s*;"),todos=Pattern.compile("(?i)\\bTODO\\b|not implemented|not yet supported|coming soon");
        try(var files=Files.walk(server.resolve("src/main"))){for(Path file:files.filter(Files::isRegularFile).sorted().toList()){
            String name=file.toString();if(!name.endsWith(".java")&&!name.endsWith(".kt"))continue;
            String relative=server.relativize(file).toString().replace('\\','/'),text=Files.readString(file),code=uncomment(text),component=group(relative);
            int lines=(int)text.lines().count(),regs=count(REG,code),empties=count(empty,code),nils=count(nulls,code),todo=count(todos,text);
            sources.add(component+"\t"+relative+"\t"+(name.endsWith(".kt")?"Kotlin":"Java")+"\t"+lines+"\t"+regs+"\t"+empties+"\t"+nils+"\t"+todo+"\t"+count(Pattern.compile("isPVP\\(|WorldType\\.PVP|\"PVP\""),code)+"\t"+count(Pattern.compile("isEco\\(|WorldType\\.ECO|\"ECO\""),code));
            int[] sum=summary.computeIfAbsent(component,k->new int[6]);sum[0]++;sum[1]+=lines;sum[2]+=regs;sum[3]+=empties;sum[4]+=nils;sum[5]+=todo;
        }}
        Files.write(out.resolve("source-files.tsv"),sources);
        List<String> groups=new ArrayList<>(List.of("component\tsource_files\tlines\tregistration_calls\tempty_method_markers\treturn_null_markers\ttodo_markers"));
        summary.forEach((key,n)->groups.add(key+"\t"+n[0]+"\t"+n[1]+"\t"+n[2]+"\t"+n[3]+"\t"+n[4]+"\t"+n[5]));Files.write(out.resolve("components.tsv"),groups);
        List<String> data=new ArrayList<>(List.of("path\tformat\tstatus\ttop_level_records\tpvp_compatible_rows\teco_only_rows"));
        ObjectMapper yaml=new ObjectMapper(new YAMLFactory());int invalid=0;
        try(var files=Files.walk(server.resolve("data"))){for(Path file:files.filter(Files::isRegularFile).sorted().toList()){
            String name=file.toString();if(!name.endsWith(".json")&&!name.endsWith(".yaml")&&!name.endsWith(".yml"))continue;
            String path=server.relativize(file).toString().replace('\\','/');
            try{
                int records=0,pvp=0,eco=0;
                if(name.endsWith(".json")){
                    JsonElement root=new JsonParser().parse(Files.readString(file));records=root.isJsonArray()?root.getAsJsonArray().size():root.isJsonObject()?root.getAsJsonObject().size():1;
                    if(root.isJsonArray())for(JsonElement value:root.getAsJsonArray())if(value.isJsonObject()){
                        JsonObject object=value.getAsJsonObject();String world=object.has("world")&&!object.get("world").isJsonNull()?object.get("world").getAsString():"";
                        if(world.isEmpty()||world.equals("PVP"))pvp++;if(world.equals("ECO"))eco++;
                    }
                }else{JsonNode root=yaml.readTree(file.toFile());records=root!=null&&root.has("defaultStock")?root.get("defaultStock").size():root==null?0:root.size();}
                data.add(path+"\t"+(name.endsWith(".json")?"JSON":"YAML")+"\tPARSED\t"+records+"\t"+pvp+"\t"+eco);
            }catch(Exception failure){invalid++;data.add(path+"\tDATA\tPARSE_ERROR_"+failure.getClass().getSimpleName()+"\t0\t0\t0");}
        }}
        Files.write(out.resolve("data-files.tsv"),data);
        String book=Files.readString(server.resolve("src/main/java/io/ruin/model/skills/magic/SpellBook.java"));
        List<String> placeholders=new ArrayList<>(List.of("spell\tstatus"));Matcher spells=Pattern.compile("new TodoSpell\\(\"([^\"]+)\"\\)").matcher(uncomment(book));
        while(spells.find())placeholders.add(spells.group(1)+"\tPLACEHOLDER_ACTION");Files.write(out.resolve("placeholder-spells.tsv"),placeholders);
        io.ruin.model.combat.CombatEquipmentInteractionTest.data();
        List<String> refs=new ArrayList<>(List.of("kind\tsource\tid\tname\tworld\tcache_definition"));
        int missing=0;
        for(String kind:new String[]{"npcs/spawns","objects/spawns"})try(var files=Files.walk(server.resolve("data/"+kind))){
            for(Path file:files.filter(p->p.toString().endsWith(".json")).sorted().toList()){
                JsonElement root=new JsonParser().parse(Files.readString(file));if(!root.isJsonArray())continue;
                for(JsonElement value:root.getAsJsonArray()){
                    if(!value.isJsonObject())continue;JsonObject object=value.getAsJsonObject();
                    if(!object.has("id")||!object.get("id").isJsonPrimitive())continue;int id=object.get("id").getAsInt();if(id<0)continue;
                    String name=object.has("name")?object.get("name").getAsString():"";
                    String world=object.has("world")&&!object.get("world").isJsonNull()?object.get("world").getAsString():"ALL";
                    boolean exists=kind.startsWith("npcs")?io.ruin.cache.NPCDef.get(id)!=null:io.ruin.cache.ObjectDef.get(id)!=null;
                    if(!exists)missing++;
                    refs.add(kind+"\t"+server.relativize(file).toString().replace('\\','/')+"\t"+id+"\t"+name.replace('\t',' ')+"\t"+world+"\t"+(exists?"FOUND":"MISSING_CACHE_ID"));
                }
            }
        }
        Files.write(out.resolve("spawn-references.tsv"),refs);
        List<String> totals=List.of("Source files inventoried: "+(sources.size()-1),"Component groups: "+summary.size(),"Data files checked: "+(data.size()-1),"Data parse failures: "+invalid,"Registered placeholder spells: "+(placeholders.size()-1),"Spawn rows checked against cache: "+(refs.size()-1),"Missing spawn cache definitions: "+missing,"Scope: source/data inventory, not a live playthrough or proof of correctness.");
        Files.write(out.resolve("inventory-summary.txt"),totals);totals.forEach(System.out::println);
        System.exit(0);
    }
}
