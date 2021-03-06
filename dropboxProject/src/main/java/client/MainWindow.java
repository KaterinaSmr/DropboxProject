package client;

import common.FilesTree;
import common.MyObjectInputStream;
import common.ServerCommands;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.robot.Robot;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Exchanger;
import java.util.concurrent.atomic.AtomicInteger;

public class MainWindow implements ServerCommands {
    @FXML
    public TreeView <FilesTree> treeView;
    @FXML
    public TableView <FilesTree> tableView;
    @FXML
    private TableColumn<FilesTree, String> columnName;
    @FXML
    private TableColumn<FilesTree, String> columnType;
    @FXML
    private TableColumn<FilesTree, Long> columnSize;
    @FXML
    private TableColumn<FilesTree, String> columnTime;
    @FXML
    private Button downloadButton;
    @FXML
    private Button uploadButton;
    @FXML
    private Button refreshButton;
    @FXML
    private Button addFolderButton;
    @FXML
    private Button removeButton;
    @FXML
    private Button renameButton;
    @FXML
    private Button logoutButton;

    private Image iconFolder;
    private SocketChannel socketChannel;
    private MyObjectInputStream inObjStream;
    private FilesTree filesTree = null;
    private Exchanger<String> statusExchanger;
    private MessageWindow messageWindow;
    private AtomicInteger newFolderLock;

    public void main(){
        System.out.println("Method start() is called");
        statusExchanger = new Exchanger<>();
        setupVisualElements();

        try {
            requestFilesTreeRefresh();
        } catch (Exception e) {
            e.printStackTrace();
        }

       new Thread(()->{
            try {
                while (true) {
                    String header = readMessageHeader(COMMAND_LENGTH);
                    System.out.println("Header: " + header);
                    if (header.startsWith(FILES_TREE)){
                        int objectSize = Integer.parseInt(header.split(SEPARATOR)[1]);
                        System.out.println("Object size " + objectSize);
                        try {
                            filesTree = (FilesTree) inObjStream.readObject(objectSize);
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                        Platform.runLater(()->{
                            refreshFilesTreeAndTable(filesTree);
                        });
                    } else if (header.startsWith(RENAMSTATUS) || header.startsWith(REMSTATUS) || header.startsWith(NEWFOLDSTATUS)){
                        String msg = readMessage();
                        try {
                            statusExchanger.exchange(msg);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } else if (header.startsWith(INFO)){
                        String msg = readMessage();
                        messageWindow.show("Info", msg, MessageWindow.Type.INFORMATION);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private String readMessage() throws IOException{
        ByteBuffer buffer = ByteBuffer.allocate(128);
        socketChannel.read(buffer);
        buffer.flip();
        String s = "";
        while (buffer.hasRemaining()){
            s += (char) buffer.get();
        }
        return s;
    }

    public void setSocketChannel(SocketChannel socketChannel) throws IOException {
        this.socketChannel = socketChannel;
        inObjStream = new MyObjectInputStream(socketChannel);
    }

    private String readMessageHeader(int bufferSize) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
        String s = "";
        socketChannel.read(buffer);
        buffer.flip();
        while (buffer.hasRemaining()){
            s += (char) buffer.get();
        }
        return s;
    }

    public void refreshFilesTreeAndTable(FilesTree rootNode) {
        tableView.getItems().clear();
        System.out.println("refresh");
        TreeItem<FilesTree> rootItem = buildTreeView(rootNode);
        treeView.setRoot(rootItem);
    }

    private void setSelectedTreeItem (TreeItem<FilesTree> item){
        treeView.getSelectionModel().select(item);
        item.setExpanded(true);
        selectItem();
    }

    private TreeItem<FilesTree> getTreeItemByValue (TreeItem<FilesTree> treeRoot, FilesTree value){
        TreeItem<FilesTree> result;
        if (treeRoot.getValue() != null && treeRoot.getValue().equals(value))
            return treeRoot;
        else {
            for (TreeItem<FilesTree> child : treeRoot.getChildren()) {
                result = getTreeItemByValue(child, value);
                if (result != null) return result;
            }
        }
        return null;
    }

    public TreeItem<FilesTree> buildTreeView (FilesTree node){
        if (node.isDirectory()) {
            TreeItem<FilesTree> item = new TreeItem<>(node, new ImageView(iconFolder));
            for (FilesTree f : node.getChildren()) {
                if (f.isDirectory())
                    item.getChildren().add(buildTreeView(f));
            }
            return item;
        }
        return null;
    }
    private void requestFilesTreeRefresh(){
        send(GETFILELIST);
    }
    private void requestRename(String path, String newName){
        String str = RENAME + SEPARATOR + path + SEPARATOR + newName;
        send(str);
    }
    private void requestRemove(String path){
        send(REMOVE + SEPARATOR + path);
    }

    private void removeItem(FilesTree nodeToRemove){
        // ?????????????? ???????? ???? FilesTree
        String parent = nodeToRemove.getFile().getParent();
        filesTree.removeChild(nodeToRemove);
        //?????????????? ???? ????????????
        TreeItem itemToRemove = getTreeItemByValue(treeView.getRoot(), nodeToRemove);
        if (itemToRemove != null) {
            itemToRemove.getParent().getChildren().remove(itemToRemove);
            treeView.refresh();
        }
        //?????????????? ???? ??????????????
        tableView.getItems().removeAll();
        selectItem();
    }

    private void send(String s) {
        ByteBuffer buffer = null;
        try {
            buffer = ByteBuffer.wrap(s.getBytes());
            socketChannel.write(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        buffer.clear();
    }
    @FXML
    public void selectItem(){
        TreeItem<FilesTree> item = (TreeItem<FilesTree>) treeView.getSelectionModel().getSelectedItem();
        if (item != null) {
            tableView.getItems().clear();
            FilesTree node = item.getValue();
            for (FilesTree f:node.getChildren()) {
                tableView.getItems().add(f);
            }
            tableView.sort();
        }
    }

    @FXML
    public void onDownloadButton(){}
    @FXML
    public void onUploadButton(){}
    @FXML
    public void onRenameButton() {
        if (tableView.getSelectionModel().getSelectedCells().isEmpty()) {
            messageWindow.show("Warning", "No files selected", MessageWindow.Type.INFORMATION);
            return;
        }
        tableView.requestFocus();
        Robot robot = new Robot();
        robot.keyPress(KeyCode.ENTER);
    }
    @FXML
    public void onRemoveButton(){
        FilesTree nodeToRemove;
        try {
            if (tableView.getSelectionModel().getSelectedCells().isEmpty()) {
                messageWindow.show("Warning", "No files selected", MessageWindow.Type.INFORMATION);
                return;
            } else {
                TablePosition pos = tableView.getSelectionModel().getSelectedCells().get(0);
                nodeToRemove = tableView.getItems().get(pos.getRow());
                if (nodeToRemove.equals(filesTree)) {
                    messageWindow.show("Removal failed","Cannot remove root folder", MessageWindow.Type.INFORMATION);
                    return;
                }
                messageWindow.show("Please confirm", "Are you sure to remove '" + nodeToRemove.getName() + "'?", MessageWindow.Type.CONFIRMATION);
            }
            boolean toRemove = messageWindow.getResult();
            if (!toRemove) return;
            requestRemove(nodeToRemove.getFile().getAbsolutePath());
            String resp = "";
            resp = statusExchanger.exchange("ok");
            if (resp.startsWith(OK)) {
                removeItem(nodeToRemove);
            } else {
                System.out.println(resp);
                messageWindow.show("Removal failed", resp, MessageWindow.Type.INFORMATION);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    @FXML
    public void onRefreshButton(){
        requestFilesTreeRefresh();
    }
    @FXML
    public void onAddFolderButton(){
        InputWindow inputWindow = new InputWindow();
        inputWindow.show("New Folder","Please enter folder name", "New Folder");
        String name = inputWindow.getResult();
        System.out.println("New folder name: " + name);
        if (name == null) return;

        TreeItem<FilesTree> item = (TreeItem<FilesTree>) treeView.getSelectionModel().getSelectedItem();
        FilesTree parent = item.getValue();
        send(NEWFOLDER + SEPARATOR + parent.getFile().getAbsolutePath()
                + SEPARATOR + name);

        String resp = "";
        try {
            resp = statusExchanger.exchange("ok");
            System.out.println("thread Main got msg: " + resp);
            if (resp.startsWith(OK)) {
                String[] newPath = resp.split(SEPARATOR);
                File newFile = new File(newPath[1]);
                FilesTree newNode = new FilesTree(newFile, true);
                parent.addChild(newNode);
                TreeItem<FilesTree> newTreeItem = new TreeItem<>(newNode, new ImageView(iconFolder));
                treeView.getSelectionModel().getSelectedItem().getChildren().add(newTreeItem);
                treeView.refresh();
                setSelectedTreeItem(newTreeItem.getParent());
            } else {
                System.out.println(resp);
                messageWindow.show("Creation failed", resp, MessageWindow.Type.INFORMATION);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
    @FXML
    public void onLogoutButton(){

    }
    @FXML
    public void keyboardHandler(KeyEvent ke){
        if (ke.getCode().equals(KeyCode.DELETE))
            onRemoveButton();
    }

    public void onExit(){
        try {
            send(END);
            socketChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Main Window is closing");
    }

    public void setupVisualElements(){
        newFolderLock = new AtomicInteger(0);
        messageWindow = new MessageWindow();
        iconFolder = new Image(getClass().getResourceAsStream("folder_icon.png"),20,20,true,false);
        Image iconDownload = new Image(getClass().getResourceAsStream("download_icon.png"),48,48,true,false);
        downloadButton.setGraphic(new ImageView(iconDownload));
        Image iconUpload = new Image(getClass().getResourceAsStream("upload_icon.png"),48,48,true,false);
        uploadButton.setGraphic(new ImageView(iconUpload));
        Image iconAddFolder = new Image(getClass().getResourceAsStream("addFolder_icon.png"),48,48,true,false);
        addFolderButton.setGraphic(new ImageView(iconAddFolder));
        Image iconLogout = new Image(getClass().getResourceAsStream("logout_icon.png"),48,48,true,false);
        logoutButton.setGraphic(new ImageView(iconLogout));
        Image iconRefresh = new Image(getClass().getResourceAsStream("refresh_icon.png"),48,48,true,false);
        refreshButton.setGraphic(new ImageView(iconRefresh));
        Image iconRemove = new Image(getClass().getResourceAsStream("remove_icon.png"),48,48,true,false);
        removeButton.setGraphic(new ImageView(iconRemove));
        Image iconRename = new Image(getClass().getResourceAsStream("rename_icon.png"),48,48,true,false);
        renameButton.setGraphic(new ImageView(iconRename));


        columnName.setCellValueFactory(new PropertyValueFactory<>("name"));
        columnType.setCellValueFactory(new PropertyValueFactory<>("type"));
        columnSize.setCellValueFactory(new PropertyValueFactory<>("size"));
        columnTime.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        tableView.getSortOrder().add(columnType);
        columnName.setCellFactory(TextFieldTableCell.<FilesTree>forTableColumn());

        columnName.setOnEditCommit(
                (EventHandler<TableColumn.CellEditEvent<FilesTree, String>>) t -> {
                    System.out.println("On edit commit called");
                    FilesTree changedNode = ((FilesTree) t.getTableView().getItems().get(t.getTablePosition().getRow()));
                    String s = t.getNewValue();

                    if (changedNode.equals(filesTree)){
                        messageWindow.show("Rename failed", "Cannot rename root folder", MessageWindow.Type.INFORMATION);
                        return;
                    }
                    if (s.equals(changedNode.getName())) {
                        System.out.println("Name is the same");
                        messageWindow.show("Rename failed", "New name is the same", MessageWindow.Type.INFORMATION);
                        return;
                    }
                    requestRename(changedNode.getFile().getAbsolutePath(), s);
                    String resp = "";
                    try {
                        resp = statusExchanger.exchange("ok");
                        System.out.println("thread Main got msg: " + resp);
                        if (resp.startsWith(OK)) {
                            String[] newPath = resp.split(SEPARATOR);
                            File newFile = new File(newPath[1]);
                            changedNode.setFile(newFile);
                            treeView.refresh();
                        } else {
                            System.out.println(resp);
                            messageWindow.show("Rename failed", resp, MessageWindow.Type.INFORMATION);
                            tableView.requestFocus();
                            Robot robot = new Robot();
                            robot.keyPress(KeyCode.ENTER);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
        );

        //?????? ???? ???????????? ??????, ?????????? ?????? ?????????????? ?????????? ???? ?????????? ?? ?????????????????? ?????????? ?????????????????????????? ??????????????????
        // ?????????????????????????????? ???????? ???????????? ?????????????????? ?? ???? ?????????????????????????? ?? ?????? ??????????
        tableView.setRowFactory( tv -> {
            TableRow<FilesTree> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() >= 2) {
                    FilesTree selectedFilesTreeNode = row.getItem();
                    TreeItem<FilesTree> selectedTreeItem = getTreeItemByValue(treeView.getRoot(), selectedFilesTreeNode);
                    if (selectedFilesTreeNode.isDirectory())
                        setSelectedTreeItem(selectedTreeItem);
                }
            });
            return row ;
        });
    }

}
