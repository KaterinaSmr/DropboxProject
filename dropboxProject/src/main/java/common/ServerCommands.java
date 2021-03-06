package common;

public interface ServerCommands {
    String AUTH = "/reqauthoriz";
    String GETFILELIST = "/reqfilelist";
    String RENAME = "/reqrename";
    String RENAMSTATUS = "/renamstatus";
    String REMOVE = "/reqtoremove";
    String REMSTATUS = "/removalstat";
    String NEWFOLDER = "/reqnewfoldr";
    String NEWFOLDSTATUS = "/newfoldstat";
    String OK = "OK";
    String AUTHOK ="/authorizaok";
    String INFO = "/infomessage";
    String SEPARATOR = "//";
    String END = "/end";
    String FILES_TREE = "/tree";
    int COMMAND_LENGTH = 12;
}
