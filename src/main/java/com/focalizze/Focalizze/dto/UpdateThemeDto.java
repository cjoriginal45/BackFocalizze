package com.focalizze.Focalizze.dto;

public record UpdateThemeDto(
        String backgroundType, // 'default', 'color', 'image'
        String backgroundValue // null, hex o url
) {
}
