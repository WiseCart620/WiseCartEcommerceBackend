package com.wisecartecommerce.ecommerce.enums;


    public enum Jnt_Tracking_Status {
    AWAITING_PICKUP,   // order placed, tracking number not yet entered
    PICKED_UP,         // admin entered tracking number + marked picked up
    IN_TRANSIT,
    OUT_FOR_DELIVERY,
    DELIVERED,
    RETURNED
}
    

