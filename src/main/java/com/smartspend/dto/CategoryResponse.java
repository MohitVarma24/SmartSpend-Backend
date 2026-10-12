package com.smartspend.dto;

public class CategoryResponse {
    private Long id;
    private String name;
    private String icon;
    private String color;
    private boolean custom;

    public CategoryResponse() {}

    public CategoryResponse(Long id, String name, String icon, String color, boolean custom) {
        this.id = id;
        this.name = name;
        this.icon = icon;
        this.color = color;
        this.custom = custom;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public boolean isCustom() { return custom; }
    public void setCustom(boolean custom) { this.custom = custom; }
}
