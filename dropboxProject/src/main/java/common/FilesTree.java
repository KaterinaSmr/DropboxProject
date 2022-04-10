package common;

import java.io.File;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

public class FilesTree implements Serializable {
    ArrayList<FilesTree> children;
    File file;

    String name;
    String type;
    Long size;
    String timestamp;

    static DateFormat formatter = new SimpleDateFormat("dd.MM.yy HH:mm");
    static {
        formatter.setTimeZone(TimeZone.getTimeZone("GMT+3"));
    }

    public FilesTree(File f) {
        this.file = f;
        this.children = new ArrayList<>();
        this.name = f.getName();
        this.type = (isDirectory()? "Folder" : "File");
        this.size = f.length();

        Date date = new Date(f.lastModified());
        this.timestamp = formatter.format(date);

        if (this.isDirectory()) {
            File[] files = f.listFiles();
            for (File fl : files) {
                this.addChild(new FilesTree(fl));
            }
        }
    }
    public boolean isDirectory(){
        return file.isDirectory();
    }
    public boolean hasChildren (){
        return !children.isEmpty();
    }
    public void addChild(FilesTree child){
        children.add(child);
    }
    public File getFile(){
        return file;
    }
    public String getName(){
        return file.getName();
    }
    public void printNode(int level){
        System.out.println(level + " | " + this.getName());
        if (this.hasChildren()){
            for (FilesTree f: this.children) {
                int l = level+1;
                f.printNode(l);
            }
        }
    }

    @Override
    public String toString() {
        return this.getName();
    }

    public ArrayList<FilesTree> getChildren() {
        return children;
    }

    public String getType() {
        return type;
    }

    public Long getSize() {
        return size;
    }

    public String getTimestamp() {
        return timestamp;
    }
}