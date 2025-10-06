package com.example.E_shopping.util;

import com.example.E_shopping.Entity.Permission;

import java.util.Set;

public class RolePermission {

    public static Set<Permission> getPermissions(String role) {
        return switch (role) {
            case "USER" -> Set.of(
                    Permission.VIEW_DESCRIPTION,
                    Permission.VIEW_PRICE,
                    Permission.VIEW_COLOR
            );
            case "MERCHANT" -> Set.of(
                    Permission.ADD_PRODUCT,
                    Permission.UPDATE_PRODUCT,
                    Permission.DELETE_PRODUCT,
                    Permission.MANAGE_INVENTORY
            );
            default -> Set.of();
        };
    }
}
