package com.nis.modules.sys.utils;

import com.nis.common.utils.Tool;

/**
 * HaspStatus Enumeration
 * https://docs.sentinel.thalesgroup.com/ldk/LDKdocs/API-admin-java/Admin_API/Admin_API_Return_Codes.htm
 *
 * @author shizhendong
 * @date 2023-09-13
 * @see SafeNet.AdminApi
 * @see SafeNet.AdminStatus
 */
public enum AdminStatusEnum {

    SNTL_ADMIN_STATUS_OK(0, "Request was successfully completed"),
    SNTL_ADMIN_INSUF_MEM(3, "System is out of memory"),
    SNTL_ADMIN_INVALID_CONTEXT(6001, "The License Manager context is corrupt or contains invalid data"),
    SNTL_ADMIN_LM_NOT_FOUND(6002, "The specified License Manager could not be found"),
    SNTL_ADMIN_LM_TOO_OLD(6003, "The specified License Manager was found, but it is an earlier version that does not support Sentinel Admin API functions"),
    SNTL_ADMIN_BAD_PARAMETERS(6004, "The XML request code contains errors. See the resulting information XML for details"),
    SNTL_ADMIN_LOCAL_NETWORK_ERR(6005, "An error occurred while the API was initializing the local network interface"),
    SNTL_ADMIN_CANNOT_READ_FILE(6006, "An error occurred when attempting to read the file specified by the prefix \"file://\". The file does not exist or cannot be accessed"),
    SNTL_ADMIN_SCOPE_ERROR(6007, "The scope definition in the <haspscope> block contains an error"),
    SNTL_ADMIN_PASSWORD_REQUIRED(6008, "Password is required"),
    SNTL_ADMIN_CANNOT_SET_PASSWORD(6009, "Error setting new password"),
    SNTL_ADMIN_UPDATE_ERROR(6010, "Error applying an update (see reply for further explanation)"),
    SNTL_ADMIN_LOCAL_ONLY(6011, "The specified License Manager was found, but the License Manager is configured to block remote access"),
    SNTL_ADMIN_BAD_VALUE(6012, "An XML element tried to set an illegal value"),
    SNTL_ADMIN_READ_ONLY(6013, "When the return status is SNTL_ADMIN_BAD_PARAMETERS, this error code may be delivered in the result XML when trying to set a read-only value. For example:\n" +
            "\n" +
            "<config>\n" +
            "<ini_timestamp>1349882141</ini_timestamp>\n" +
            "</config>\n" +
            "\n" +
            " \n" +
            "\n" +
            "<admin_response>\n" +
            "<admin_status>\n" +
            "<code>6004</code>\n" +
            "<text>ERROR 6004</text>\n" +
            "<numerrors>1</numerrors>\n" +
            "<error>SNTL_ADMIN_READ_ONLY (6013) in ini_timestamp</error>\n" +
            "</admin_status>\n" +
            "</admin_response>"),
    SNTL_ADMIN_ELEMENT_UNDEFINED(6014, "When the return status is SNTL_ADMIN_BAD_PARAMETERS, this error code may be delivered in the result XML when trying to request a non-existent value. For example:\n" +
            "\n" +
            "<admin>\n" +
            "<hasp>\n" +
            "<element name=\"haspid\" />\n" +
            "<element name=\"temperature\" />\n" +
            "</hasp>\n" +
            "</admin>\n" +
            "\n" +
            "<admin_response>\n" +
            "<hasp>\n" +
            "<haspid>305420012</haspid>\n" +
            "</hasp>\n" +
            "<hasp>\n" +
            "<haspid>1141301516</haspid>\n" +
            "</hasp>\n" +
            "<admin_status>\n" +
            "<code>6004</code>\n" +
            "<text>ERROR 6004</text>\n" +
            "<numerrors>1</numerrors>\n" +
            "<error>SNTL_ADMIN_ELEMENT_UNDEFINED (6014): temperature</error>\n" +
            "</admin_status>\n" +
            "</admin_response>"),
    SNTL_ADMIN_INVALID_PTR(6015, "Get method: The format or scope parameter is NULL.\n" +
            "\n" +
            "Set method: The input parameter is NULL"),
    SNTL_ADMIN_NO_INTEGRATED_LM(6016, "In standalone Admin API only: Context was created to address the Integrated License Manager (sntl_integrated_lm). However, the Integrated License Manager is only available with the Integrated Admin API"),
    SNTL_ADMIN_RESULT_TOO_BIG(6017, "Returned result is too big. Use a scope to narrow the results"),
    SNTL_ADMIN_SCOPE_RESULTS_EMPTY(6019, "Unable to locate any items that match the scope."),
    SNTL_ADMIN_INV_VCODE(6022, "Invalid Vendor Code was passed."),
    SNTL_ADMIN_UNKNOWN_VCODE(6034, "The Vendor Code is not recognized by the API"),
    SNTL_ADMIN_ENTROPY_SOURCE_FAILED(6035, "SNTL_ADMIN_ENTROPY_SOURCE_FAILED"),
    SNTL_ADMIN_INV_CERT(6036, "SNTL_ADMIN_INV_CERT"),
    SNTL_ADMIN_CERT_VERIFY_FAILED(6037, "SNTL_ADMIN_CERT_VERIFY_FAILED"),
    SNTL_ADMIN_CONNECT_MISSING(6051, "Connect method is not called."),
    SNTL_ADMIN_DLL_BROKEN(6052, "Java JNI DLL found broken."),
    SNTL_ADMIN_DLL_LOADED_IN_ANOTHER_CLASSLOADER(6053, "Java JNI DLL loaded in another class loader."),
    SNTL_ADMIN_IDENTITY_CODE_REQUIRED(6054, "The Set function was called to update or delete a client identity or to unregister a machine. However, the identity_code element is not present or contains an empty value"),
    SNTL_ADMIN_ISSUED_TO_REQUIRED(6055, "The Set function was called to create a new client identity. However, the issued_to element is not present or contains an empty value."),
    SNTL_ADMIN_MAXIMUM_NUMBER_OF_AUTO_REGISTERED_MACHINES_REQUIRED(6056, "The Set function was called to create a new client identity. However, the maximum_number_of_auto_registered_machines element is not present or contains an empty value."),
    SNTL_ADMIN_IDENTITY_CODE_DOES_NOT_EXIST(6057, "The Set function was called to perform an update, delete or unregister action for an existing client identity. However, the specified identity_code does not exist in the database."),
    SNTL_ADMIN_IDENTITY_CODE_CREATION_FAILED(6058, "An identity code or secret failed to generate."),
    SNTL_ADMIN_IDENTITY_NOTHING_TO_UNREGISTER(6059, "The Set function was called to unregister one or more registered machines. However, no machines matched the elements specified in the XML code that provided to the function."),
    UNKNOWN_STATUS(-999999, "unknown status");

    private Integer status;
    private String description;

    AdminStatusEnum(Integer status, String description) {
        this.status = status;
        this.description = description;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public static AdminStatusEnum getAdminStatusByCode(Integer code) {
        for (AdminStatusEnum adminStatusEnum : AdminStatusEnum.values()) {
            if (Tool.ObjectUtil.equals(adminStatusEnum.getStatus(), code)) {
                return adminStatusEnum;
            }
        }

        // UNKNOWN_ERROR
        AdminStatusEnum unknownStatus = AdminStatusEnum.UNKNOWN_STATUS;
        unknownStatus.setStatus(code);
        return unknownStatus;
    }

    @Override
    public String toString() {
        return this.name() + "{" +
                "status=" + status +
                ", description='" + description + '\'' +
                '}';
    }

}
