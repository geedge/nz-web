/**
 *  1. rename software_type
            TSG-OS Concentrate -> TSG-OS HL Seats
            TSG-OS Stand-alone -> TSG-OS HL
 */

UPDATE software_type SET `name` = "TSG-OS HL Seats" WHERE `name` = "TSG-OS Concentrate";
UPDATE software_type SET `name` = "TSG-OS HL" WHERE `name` = "TSG-OS Stand-alone";
