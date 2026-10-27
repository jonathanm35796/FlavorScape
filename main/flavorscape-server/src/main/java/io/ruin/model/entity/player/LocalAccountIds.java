package io.ruin.model.entity.player;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Persistent, collision-free identities for the file-backed login service. */
public final class LocalAccountIds {
    private static final Gson JSON = new Gson();
    private final Path file;
    private Map<String,Integer> ids;
    public LocalAccountIds(Path file) { this.file=file; }
    public synchronized int getOrCreate(String name) throws IOException {
        if(name==null||name.trim().isEmpty())throw new IOException("Missing account name");
        if(ids==null) {
            try {
                ids=Files.exists(file)?JSON.fromJson(Files.readString(file),new TypeToken<Map<String,Integer>>(){}.getType()):new TreeMap<>();
                if(ids==null)throw new IllegalArgumentException("Empty identity registry");
                Set<Integer> used=new HashSet<>();
                for(Map.Entry<String,Integer> entry:ids.entrySet())
                    if(entry.getValue()==null||entry.getValue()<=0||!used.add(entry.getValue()))
                        throw new IllegalArgumentException("Invalid or duplicate account identity");
            }catch(RuntimeException failure){ids=null;throw new IOException("Invalid local account identity registry",failure);}
        }
        String key=name.toLowerCase(Locale.ROOT);
        Integer existing=ids.get(key);if(existing!=null)return existing;
        int highest=ids.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        if(highest==Integer.MAX_VALUE)throw new IOException("Account identity space exhausted");
        Map<String,Integer> next=new TreeMap<>(ids);next.put(key,highest+1);
        Path directory=file.toAbsolutePath().getParent();Files.createDirectories(directory);
        Path temporary=Files.createTempFile(directory,"account-ids-",".tmp");
        try {
            Files.writeString(temporary,JSON.toJson(next),StandardCharsets.UTF_8);
            try {Files.move(temporary,file.toAbsolutePath(),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            catch(AtomicMoveNotSupportedException ignored){Files.move(temporary,file.toAbsolutePath(),StandardCopyOption.REPLACE_EXISTING);}
        }finally{Files.deleteIfExists(temporary);}
        ids=next;return highest+1;
    }
}
