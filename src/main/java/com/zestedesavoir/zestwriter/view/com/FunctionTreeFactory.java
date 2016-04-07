package com.zestedesavoir.zestwriter.view.com;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import com.zestedesavoir.zestwriter.model.Container;
import com.zestedesavoir.zestwriter.model.ContentNode;
import com.zestedesavoir.zestwriter.model.Extract;
import com.zestedesavoir.zestwriter.model.MetaContent;
import com.zestedesavoir.zestwriter.model.Textual;
import com.zestedesavoir.zestwriter.view.dialogs.EditContentDialog;

import javafx.application.Platform;
import javafx.collections.ObservableMap;
import javafx.event.Event;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TreeItem;
import javafx.util.Pair;

public class FunctionTreeFactory {

    public static Map<String,Object> initContentDialog(Map<String, Object> defaultParam) {
        if(defaultParam == null) {
            defaultParam = new HashMap<>();
            defaultParam.put("title", "");
            defaultParam.put("description", "");
            defaultParam.put("type", EditContentDialog.typeOptions.get(1));
            defaultParam.put("licence", EditContentDialog.licOptions.get(6));
        }
        // Create wizard
        EditContentDialog dialog = new EditContentDialog(defaultParam);

        Optional<Pair<String, Map<String, Object>>> result = dialog.showAndWait();
        if(result.isPresent()) {
            return result.get().getValue();
        } else {
            return null;
        }

     }

    public static void clearContent(ObservableMap<Textual, Tab> extracts, TabPane editorList) {
        for(Entry<Textual, Tab> entry:extracts.entrySet()) {
            Platform.runLater(() -> {
                Event.fireEvent(entry.getValue(), new Event(Tab.TAB_CLOSE_REQUEST_EVENT));
                Event.fireEvent(entry.getValue(), new Event(Tab.CLOSED_EVENT));
                editorList.getTabs().remove(entry.getValue());
            });
        }
        extracts.clear();
    }

    public static TreeItem<ContentNode> buildChild(TreeItem<ContentNode> node) {
        if(node.getValue() instanceof Container) {
            Container container = (Container) node.getValue();
            TreeItem<ContentNode> itemIntro = new TreeItem<>((ContentNode) container.getIntroduction());
            node.getChildren().add(itemIntro);
            for(MetaContent child:container.getChildren()) {
                TreeItem<ContentNode> itemChild = new TreeItem<>((ContentNode) child);
                node.getChildren().add(buildChild(itemChild));
            }
            TreeItem<ContentNode> itemConclu = new TreeItem<>((ContentNode) container.getConclusion());
            node.getChildren().add(itemConclu);
        }
        else if(node.getValue() instanceof Extract) {
            Extract extract = (Extract) node.getValue();
            TreeItem<ContentNode> itemExtract = new TreeItem<>((ContentNode) extract);
            return itemExtract;
        }
        return node;
    }

    public static void moveToContainer(TreeItem<ContentNode> dest, TreeItem<ContentNode> src) {
        // remove in model
        ((Container) src.getParent().getValue()).getChildren().remove(src.getValue());
        // remove in ui
        src.getParent().getChildren().remove(src);

        if (dest.getValue() instanceof Container) {
            Container destination = (Container)dest.getValue();
            int position = destination.getChildren().size();
            // update model
            destination.getChildren().add(position, (MetaContent) src.getValue());
            //update ui
            dest.getChildren().add(position + 1, src);

        } else {
            Container destParent = (Container) dest.getParent().getValue();
            int position = destParent.getChildren().indexOf(dest.getValue());
            // update model
            destParent.getChildren().add(position + 1, (MetaContent)src.getValue());
            // update ui
            System.out.println("position = "+position);
            dest.getParent().getChildren().add(position + 2, src);
        }
    }

}
