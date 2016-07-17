package com.zestedesavoir.zestwriter.contents.plugins;


import com.zestedesavoir.zestwriter.MainApp;
import com.zestedesavoir.zestwriter.utils.Configuration;
import com.zestedesavoir.zestwriter.view.com.FunctionTreeFactory;
import com.zestedesavoir.zestwriter.view.com.IconFactory;
import javafx.scene.control.Alert;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Objects;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class PluginsLoader{
    private MainApp mainApp;
    private Configuration config;
    private Logger logger;
    private ArrayList<Plugin> plugins = new ArrayList<>();

    public PluginsLoader(MainApp mainApp){
        this.mainApp = mainApp;
        this.config = MainApp.getConfig();

        logger = LoggerFactory.getLogger(PluginsLoader.class);
    }

    public ArrayList<Plugin> getPlugins(){
        logger.debug("[PLUGINS] Starting loading plugins");
        File pluginsFile[];

        File pluginFolder = new File(config.getContentsPath() + "/plugins");
        pluginsFile = pluginFolder.listFiles();


        if(pluginsFile != null){
            logger.debug("[PLUGINS] Start list of plugins");
            for(File pluginFile : pluginsFile){
                if(FilenameUtils.getExtension(pluginFile.getPath()).equalsIgnoreCase("jar"))
                    logger.debug("[PLUGINS]   " + pluginFile.getName() + " <" + pluginFile.getPath() + ">");
            }
            logger.debug("[PLUGINS] End list of plugins");

            String mainClass = "";
            URL[] url = new URL[1];

            for(File pluginFile : pluginsFile){
                if(!FilenameUtils.getExtension(pluginFile.getPath()).equalsIgnoreCase("jar"))
                    continue;

                try{
                    url[0] = new URL("file:///" + pluginFile.getPath());
                }catch(MalformedURLException e){
                    logger.error(e.getMessage(), e);
                }

                JarFile jarFile = null;
                try{
                    jarFile = new JarFile(pluginFile);

                    Manifest manifest = jarFile.getManifest();
                    Attributes attrs = manifest.getMainAttributes();

                    for(Object o : attrs.keySet()){
                        Attributes.Name attrName = (Attributes.Name)o;
                        String attrValue = attrs.getValue(attrName);

                        if(Objects.equals(attrName.toString(), "Main-Class"))
                            mainClass = attrValue;
                    }
                }catch(IOException e){
                    logger.error("Error for opening jar file: " + pluginFile.getPath() + " -- " + e.getMessage(), e);
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setHeaderText("Une erreur est survenu lors de l'ouverture du plugin <" + pluginFile.getName() + ">");
                    alert.setContentText("Pour plus de détail, veuillez consulter le fichier de log");
                    IconFactory.addAlertLogo(alert);
                    FunctionTreeFactory.addTheming(alert.getDialogPane());
                    alert.showAndWait();
                }


                if(! mainClass.isEmpty()){
                    try{
                        URLClassLoader child = new URLClassLoader(url, this.getClass().getClassLoader());
                        Class classToLoad = Class.forName(mainClass, true, child);

                        Plugin plugin = new Plugin(mainApp, pluginFile.getName(), classToLoad);
                        plugins.add(plugin);
                    }catch(Exception e){
                        logger.error(e.getMessage(), e);
                    }
                }else{
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Plugin error");
                    alert.setHeaderText("Plugin");
                    alert.setContentText("Unable to load <" + pluginFile.getName() + ">, the Main-Class has not ben founded in Manifest file");
                }
            }

            return plugins;
        }else{
            logger.debug("[PLUGINS] No plugin founded in " + pluginFolder.getPath());
        }

        return new ArrayList<>();
    }
}
