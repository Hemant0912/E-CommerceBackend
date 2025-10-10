package com.example.E_shopping.Entity;

public enum Permission {

    // user per
    VIEW_DESCRIPTION,
    VIEW_PRICE,
    VIEW_COLOR,

    // merchant per
    ADD_PRODUCT,
    UPDATE_PRODUCT,
    DELETE_PRODUCT,
    MANAGE_INVENTORY,

    // admin per
    VIEW_USERS,
    VIEW_MERCHANTS,
    VIEW_ORDERS,
    VIEW_PRODUCTS,

    // super admin per
    CREATE_ADMIN,
    MANAGE_ALL

}
