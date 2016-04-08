package com.zestedesavoir.zestwriter.model;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.zestedesavoir.zestwriter.view.com.FunctionTreeFactory;
import com.zestedesavoir.zestwriter.view.com.IconFactory;

import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIconView;

@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.PROPERTY, property="type", visible=true)
@JsonSubTypes({@Type(value = Container.class, name = "null"), @Type(value = Content.class, name = "TUTORIAL"), @Type(value = Content.class, name = "ARTICLE") })
public class Container extends MetaContent implements ContentNode {
    private Textual _introduction;
    private Textual _conclusion;
    private List<MetaContent> _children;


    @JsonCreator
    public Container(@JsonProperty("object") String object, @JsonProperty("slug") String slug, @JsonProperty("title") String title, @JsonProperty("introduction") String introduction, @JsonProperty("conclusion") String conclusion,
            @JsonProperty("children") List<MetaContent> children) {
        super(object, slug, title);
        this._introduction = new MetaAttribute(introduction, "Introduction");
        this._conclusion = new MetaAttribute(conclusion, "Conclusion");
        this._children = children;
    }

    public Container(String object, String slug, String title, Textual introduction, Textual conclusion,
            List<MetaContent> children) {
        super(object, slug, title);
        this._introduction = introduction;
        this._conclusion = conclusion;
        this._children = children;
    }

    public Container(String object, String slug, String title, String basePath, Textual introduction,
            Textual conclusion, List<MetaContent> children) {
        super(object, slug, title, basePath);
        this._introduction = introduction;
        this._conclusion = conclusion;
        this._children = children;
    }

    public Container(String object, String slug, String title, String basePath, String introduction,
            String conclusion, ArrayList<MetaContent> children) {
        super(object, slug, title, basePath);
        this._introduction = new MetaAttribute(introduction, "Introduction");
        this._conclusion = new MetaAttribute(conclusion, "Conclusion");
        this._children = children;
    }

    public Textual getIntroduction() {
        return _introduction;
    }

    @JsonProperty(value="introduction")
    public String getJsonIntroduction() {
        return ((MetaAttribute)_introduction).getSlug();
    }

    @JsonProperty(value="conclusion")
    public String getJsonConclusion() {
        return ((MetaAttribute)_conclusion).getSlug();
    }

    public void setIntroduction(Textual introduction) {
        this._introduction = introduction;
    }

    public Textual getConclusion() {
        return _conclusion;
    }

    public void setConclusion(Textual conclusion) {
        this._conclusion = conclusion;
    }

    public List<MetaContent> getChildren() {
        return _children;
    }

    public void setChildren(List<MetaContent> children) {
        this._children = children;
    }

    public String getFilePath() {
        Path path = Paths.get(getBasePath(), getIntroduction().getFilePath());
        path = path.getParent();

        return path.toAbsolutePath().toString();
    }

    @Override
    public String toString() {
        return getTitle();
    }

    public int getCountChildrenExtract() {
        int sum = 0;
        for(MetaContent c:getChildren()) {
            if (c instanceof Extract) {
                sum++;
            }
        }
        return sum;
    }

    /**
     * This function calculates the number of ancestors type content of a node.
     * Attention, the node counts himself as his ancestor.
     * For example if you calculate the ancestors of content from itself, the result is 1.
     * @param content root content
     * @return number of container-type ancestors
     */
    public int getCountAncestorsContainer(Container content) {
        int total = 0;
        if(this.getFilePath().equals(content.getFilePath())) {
            return 1;
        }
        for(MetaContent c:content.getChildren()) {
            if (c instanceof Container) {
                total = getCountAncestorsContainer((Container) c);
                if(total > 0) {
                    return total+1;
                }
            }
        }
        return 0;
    }

    /**
     * This function calculates the number of container-type descendants of the node.
     * @return number of container-type descendants
     */
    public int getCountDescendantContainer() {
        int max =0;
        for(MetaContent c:getChildren()) {
            if (c instanceof Container) {
                max = Math.max(max, ((Container)c).getCountDescendantContainer() + 1);
            }
        }
        return max;
    }

    @Override
    public MaterialDesignIconView buildIcon() {
        return IconFactory.createFolderIcon();
    }

    @Override
    public boolean canTakeContainer(Content c) {
        return (getCountChildrenExtract() == 0 && getCountAncestorsContainer(c) < 3);
    }

    @Override
    public boolean canTakeExtract() {
        if(getCountDescendantContainer() > 0) {
            return false;
        }
        return true;
    }

    @Override
    public boolean isEditable() {
        return true;
    }

    @Override
    public boolean isMoveableIn(ContentNode receiver, Content root) {
        if(receiver instanceof MetaAttribute) {
            if(receiver.getTitle().equalsIgnoreCase("conclusion")) {
                return false;
            }
        }
        if(receiver instanceof Container) {
            int ancestors = ((Container) receiver).getCountAncestorsContainer(root);
            int descendants = getCountDescendantContainer();
            int childrenExtracts = ((Container) receiver).getCountChildrenExtract();
            if((ancestors+descendants) >=3 || childrenExtracts > 0 || receiver.getFilePath().equals(getFilePath())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof Container) {
            return getFilePath().equals(((Container)obj).getFilePath());
        }
        return super.equals(obj);
    }

    @Override
    public String exportContentToMarkdown(int level, int levelDepth) {
        StringBuilder sb = new StringBuilder();
        sb.append(FunctionTreeFactory.padding(level, '#'));
        sb.append(" ").append(getTitle()).append("\n\n");
        sb.append(FunctionTreeFactory.offsetHeaderMarkdown(getIntroduction().readMarkdown(), levelDepth)).append("\n\n");
        for(MetaContent c:getChildren()) {
            if(c instanceof Container) {
                sb.append(((Container) c).exportContentToMarkdown(level+1, levelDepth));
            } else if (c instanceof Extract) {
                sb.append(((Extract) c).exportContentToMarkdown(level +1, levelDepth));
            }
        }
        sb.append(FunctionTreeFactory.offsetHeaderMarkdown(getConclusion().readMarkdown(), levelDepth)).append("\n\n");
        return sb.toString();
    }
}