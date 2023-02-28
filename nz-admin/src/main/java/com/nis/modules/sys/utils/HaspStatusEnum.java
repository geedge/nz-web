package com.nis.modules.sys.utils;

import com.nis.common.utils.Tool;

/**
 * HaspStatus Enumeration
 * https://docs.sentinel.thalesgroup.com/softwareandservices/ldk/LDKdocs/API-licensing-net/Licensing_API/Enumerations/Licensing_API_Status_Codes.htm
 *
 * @author shizhendong
 * @date 2023-02-27
 */
public enum HaspStatusEnum {

    StatusOk(0, "Request was successfully completed"),
    InvalidAddress(1, "Request exceeds the Sentinel protection key memory range"),
    InvalidFeature(2, "Legacy HASP HL Run-time API: Unknown/Invalid Feature ID option"),
    NotEnoughMemory(3, "System is out of memory"),
    TooManyOpenFeatures(4, "Too many open sessions exist"),
    AccessDenied(5, "Access to Feature was denied"),
    IncompatibleFeature(6, "Legacy decryption method cannot work on Feature"),
    ContainerNotFound(7, "Sentinel protection key is no longer available"),
    BufferTooShort(8, "Encrypted/decrypted data length too short to execute method call"),
    InvalidHandle(9, "Invalid handle was passed to method"),
    InvalidFile(10, "Specified File ID is not recognized by API"),
    DriverTooOld(11, "Installed driver is too old to execute method"),
    NoTime(12, "Real-time clock (RTC) not available"),
    SystemError(13, "Generic error from host system call"),
    DriverNotFound(14, "Required driver is not installed"),
    InvalidFormat(15, "File format for update is not recognized"),
    RequestNotSupported(16, "Unable to execute method in this context"),
    InvalidUpdateObject(17, "Binary data that was passed to method does not contain an update"),
    KeyIdNotFound(18, "Sentinel protection key was not found"),
    InvalidUpdateData(19, "Required XML tags were not found OR Contents in binary data are missing or invalid"),
    UpdateNotSupported(20, "Update request is not supported by Sentinel protection key"),
    InvalidUpdateCounter(21, "Update counter is not set correctly"),
    InvalidVendorCode(22, "Invalid Vendor Code was passed"),
    EncryptionNotSupported(23, "Sentinel protection key does not support encryption type"),
    InvalidTime(24, "The time value that was passed is outside the supported value range"),
    NoBatteryPower(25, "The real-time clock battery has run out of power"),
    UpdateNoAckSpace(26, "Acknowledge data that was requested by the update ack_data parameter is NULL"),
    TerminalServiceDetected(27, "Program is running on a terminal server"),
    FeatureNoteImplemented(28, "Requested Feature type is not implemented"),
    UnknownAlgorithm(29, "Unknown algorithm used in V2C or V2CP file"),
    InvalidSignature(30, "Signature verification operation failed"),
    FeatureNotFound(31, "Requested Feature not found"),
    NoLog(32, "Access log not enabled"),
    LocalComErr(33, "Communication error occurred between the API and the local Sentinel License Manager"),
    UnknownVcode(34, "Vendor Code is not recognized by API"),
    InvalidXmlSpec(35, "Invalid XML specification exists"),
    InvalidXmlScope(36, "Invalid XML scope exists"),
    TooManyKeys(37, "Too many Sentinel protection keys are currently connected"),
    TooManyUsers(38, "Too many users are currently connected"),
    BrokenSession(39, "Session was interrupted. This can occur when certain updates are applied to the license while a session is active. For example:\n" +
            ">A Feature required by the session was deleted.\n" +
            ">The license was canceled.\n" +
            ">The network (remote license) support setting for a required Feature was changed. (In this case, all sessions will be interrupted, including local sessions.)\n" +
            "This may also indicate that the protection key is not available or that the user is attempting to bypass the application protection by using a protection key emulator."),
    RemoteCommErr(40, "Communication error occurred between local and remote Sentinel License Managers"),
    FeatureExpired(41, "Feature expired or no"),
    TooOldLM(42, "Sentinel License Manager version is too old. Update Sentinel Run-time Environment to the latest version."),
    DeviceError(43, "For a Sentinel SL key, an input/output error occurred in the secure storage area OR For a Sentinel HL key, a USB communication error occurred"),
    UpdateBlocked(44, "Update installation not permitted"),
    TimeError(45, "System time has been tampered with"),
    SecureChannelError(46, "Communication error occurred in the secure channel"),
    CorruptStorage(47, "Corrupt data exists in secure storage area of Sentinel protection key"),
    VendorLibNotFound(48, "The customized vendor library (haspvlib.vendorID.*) cannot be located"),
    InvalidVendorLib(49, "Unable to load Vendor library"),
    EmptyScopeResults(50, "Unable to locate any Feature that matches the scope"),
    VMDetected(51, "Protected application is running on a virtual machine, but one or more Features are not enabled for virtual machines. OR The user attempted to rehost a protection key from a physical machine to a virtual machine. However, none of the Features contained in the protection key are enabled for virtual machines."),
    HardwareModified(52, "Sentinel SL key incompatible with machine hardware. Sentinel SL key locked to different hardware. OR In the case of a V2C or V2CP file, conflict between Sentinel SL key data and machine hardware data. Sentinel SL key locked to different hardware."),
    UserDenied(53, "Login denied because of user restrictions"),
    UpdateTooOld(54, "Trying to install a V2C or V2CP file with an update counter that is out of sequence with update counter in the Sentinel protection key. Values of update counter in file are lower than those in Sentinel protection key."),
    UpdateTooNew(55, "Trying to install a V2C or V2CP file with an update counter that is out of sequence with the update counter in the Sentinel protection key. The gap between the first value in the file and the value in Sentinel protection key is greater than 1."),
    VendorLibOld(56, "Vendor library is too old"),
    UploadError(57, "Check in of a file (such as  V2C, H2R) using Admin Control Center failed, possibly because of illegal format"),
    InvalidRecipient(58, "Invalid XML RECIPIENT parameter"),
    InvalidDetachAction(59, "Invalid XML action parameter"),
    TooManyProducts(60, "The scope specified in the Transfer method does not specify a unique Product"),
    InvalidProduct(61, "Invalid Product information"),
    UnknownRecipient(62, "Update can only be applied to recipient machine specified in the Detach method, not to this machine"),
    InvalidDuration(63, "Invalid detached license duration period specified. Duration must be less than or equal to maximum allowed for this license. OR Duration extension is to a date earlier than the expiration date of the current detached license."),
    CloneDetected(64, "Cloned Sentinel SL storage was detected. Feature is unavailable."),
    UpdateAlreadyAdded(65, "The specified V2C or or V2CP update was already installed in the License Manager service"),
    HaspInactive(66, "Specified Key ID is in Inactive state"),
    NoDetachableFeature(67, "No detachable Feature exists in the specified key from which the detached license is requested"),
    TooManyHosts(68, "The specified scope does not specify a unique host"),
    RehostNotAllowed(69, "Rehost action is not allowed for the specified Key ID"),
    LicenseRehosted(70, "Original license has been transferred to another machine. Therefore, the license cannot be returned to the source machine."),
    RehostAlreadyApplied(71, "Old rehost license cannot be applied. A rehost-counter mismatch occurred"),
    CannotReadFile(72, "A V2C or V2CP file was not found, or access was denied"),
    ExtensionNotAllowed(73, "The license cannot be extended because the number of detached licenses is greater than the number of concurrent licenses allowed"),
    DetachDisabled(74, "The user attempted to detach a Product from a network license hosted on a virtual machine. However, none of the Features included in the Product are enabled for virtual machines."),
    RehostDisabled(75, "The user attempted to rehost a protection key from a virtual machine. However, none of the Features contained in the protection key are enabled for virtual machines."),
    DetachedLicenseFound(76, "The user attempted to format an SL-AdminMode key or to migrate an SL-Legacy key to an SL-AdminMode key. However, a Product is currently detached from the key."),
    RecipientOldLm(77, "For a rehost operation: The fingerprint of the target machine was collected using tools (RUS utility or Licensing API) earlier than Sentinel LDK v.7.0."),
    SecureStoreIdMismatch(78, "A secure storage ID mismatch occurred"),
    Duplicatehostname(79, "The license fingerprint is bound to a specific hostname; however, two or more machines with this hostname were found in the network. As a result, the license cannot be used."),
    MissingLM(80, "A protected application tried to log in to a Feature that supports concurrency on a Sentinel HL (Driverless configuration) key. The Sentinel LDK License Manager service is not active on the computer where the key is located."),
    FeatureInsufficientExecutionCount(81, "A protected application tried to consume or reserve multiple executions while logging in to a Feature. However, the license does not contain the number of executions that were requested."),
    ImcompatiblePlatform(82, "The function is attempting to perform an operation that is not compatible with the target platform"),
    HaspDisabled(83, "A Sentinel HL (Driverless configuration) key was disabled because a user attempted to tamper with the key or with the protected application"),
    SharingViolation(84, "An HL key is being shared by the use of a USB splitter device (such as Smart X). When this error occurs, a \"sharing violation\" message is also displayed on the relevant page in Admin Control Center."),
    KilledSession(85, "The session was interrupted manually from Admin Control Center or was interrupted due a network malfunction"),
    VsDetected(86, "The function detected that the operation is being performed in virtual storage. The machine is using computer restoration software."),
    IdentityRequired(87, "(For identity-based licenses) An identity string is required"),
    IdentityUnauthenticated(88, "(For identity-based licenses) The identity string is not authenticated"),
    IdentityDisabled(89, "(For identity-based licenses) The identity string is disabled"),
    IdentityDenied(90, "(For identity-based licenses) The client identity does not have enough permission for the operation"),
    IdentitySharingViolation(91, "(For identity-based licenses) The session was killed because the identity string used to create the session was acquired by another machine."),
    IdentityTooManyMachines(92, "(For identity-based licenses) The maximum number of machines to which the identity string can be assigned was reached"),
    IdentityServerNotReady(93, "(For identity-based licenses) The option to store identity secrets with encryption was selected in Admin Control Center, but the authentication key was not yet defined (using Sentinel Admin API)."),
    UpdateOutOfSync(94, "You attempted to install a V2C file with an update counter that is out of sync with the update counter in the Sentinel protection key. The vendor should obtain a new C2V file from the customer and then generate a new V2C file."),
    RemoteSharingViolation(95, "There were multiple attempts to access the key from a remote machine using a proxy. The user is attempting to access a license that does not support concurrency from a remote machine, using a proxy to make the remote machine appear as local."),
    CloudSessionOccupiedRemotely(96, "A cloud licensing session was released because the client requested the seat from a different location"),
    CloudMissingAuthorization(97, "Cloud licensing authorization is required to use this license"),
    InvalidNetworkSeats(98, "Invalid seat value in detach of seats with concurrency. Seat count cannot be decreased when modifying a detach"),
    NetworkDetachDisabled(99, "Detach of seats with concurrency is disabled on a Product with only unlimited-concurrency features"),
    CloudNotSupp(100, "The requested cloud functionality is not supported"),
    CloudNotTrusted(101, "Only a vendor who satisfied the documented requirements can install a license in trusted license storage"),
    CloudStorageCommErr(102, "A communication error occurred with the license storage"),
    IdentityExpired(103, "The client identity has expired"),
    InvOption(104, "The option value specified for hasp_updateSession is not valid"),
    CloudStorageBusy(105, "The license server is overloaded. The user should retry the operation in a few seconds."),
    MachineDenied(106, "Login denied because the fingerprint cannot be fetched from this machine or the fingerprint fetched from this machine is not in the correct format"),
    MachineDisabled(107, "Login denied because this machine is disabled"),
    NoApiDylib(400, "Unable to locate dynamic library for API"),
    InvApiDylib(401, "Dynamic library for API is invalid"),
    IncompleteApiDylib(402, "Requested function was not found OR In the case of the API Dispatcher, API DLL is too old"),
    InvalidObject(500, "Object was incorrectly initialized"),
    InvalidParameter(501, "Scope string is too long (maximum length is 32 KB)"),
    AreadyLoggedIn(502, "Logging in twice to same object"),
    AlreadyLoggedOut(503, "Logging out twice from same object"),
    OperationFailed(525, "Incorrect use of system or platform"),
    NoExtblock(600, "Internal error occurred: No classic memory extension block available"),
    InvPortType(650, "Internal error occurred: Invalid port type"),
    InvPort(651, "Internal error occurred: Invalid port value"),
    HaspDotNetDllBroken(652, ".NET DLL found broken"),
    NotImplemented(698, "Requested function was not implemented OR In the case of the API Dispatcher, API DLL is too old"),
    InternalError(699, "Internal error occurred in the API"),

    /**
     * unknow status
     */
    UNKNOWN_STATUS(-999999, "unknown status");

    private Integer status;
    private String description;

    HaspStatusEnum(Integer status, String description) {
        this.status = status;
        this.description = description;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static HaspStatusEnum getHaspStatusByStatus(Integer status) {
        for (HaspStatusEnum haspStatusEnum : HaspStatusEnum.values()) {
            if (Tool.ObjectUtil.equals(haspStatusEnum.getStatus(), status)) {
                return haspStatusEnum;
            }
        }

        // UNKNOWN_ERROR
        HaspStatusEnum unknownStatus = HaspStatusEnum.UNKNOWN_STATUS;
        unknownStatus.setStatus(status);
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
