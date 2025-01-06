//
//  eSIM.swift
//  OutSystemsDemoApp
//
//  Created by Andre Grillo on 09/05/2022.
//

import Foundation
import CoreTelephony

@objc(eSIM) class eSIM: CDVPlugin {
    var pluginResult = CDVPluginResult()
    var pluginCommand = CDVInvokedUrlCommand()

    @objc (isEnabled:)
    func isEnabled (_ command: CDVInvokedUrlCommand) {
        self.pluginCommand = command
        self.pluginResult = nil

       if #available(iOS 12.0, *) {
            let ctcp = CTCellularPlanProvisioning()
            let supportsESIM = ctcp.supportsCellularPlan()
            
            let resultMessage = ["isEnabled": supportsESIM]
            let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: resultMessage)
            self.commandDelegate.send(pluginResult, callbackId: command.callbackId)
        } else {
            let errorMessage = "{\"ErrorCode\" : 4, \"ErrorMessage\" : \"Device not supported. iOS version should be 12.0 or higher\"}"
            let pluginResult = CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs: errorMessage)
            self.commandDelegate.send(pluginResult, callbackId: command.callbackId)
        }
    }
    
    @objc (eSimAdd:)
    func eSimAdd (_ command: CDVInvokedUrlCommand) {
        self.pluginCommand = command
        self.pluginResult = nil
        self.pluginResult?.setKeepCallbackAs(true)

        if let activationCode = command.arguments[0] as? String {
            let activationCodeArray = activationCode.split(separator: "$")
            if activationCodeArray.count >= 3 {
                let smdpServerAddress = String(activationCodeArray[1])
                let esimMatchingID = String(activationCodeArray[2])
                if #available(iOS 12.0, *) {
                    let ctcp = CTCellularPlanProvisioning()
                    let supportsESIM = ctcp.supportsCellularPlan()
                    
                    if supportsESIM {
                        let ctpr = CTCellularPlanProvisioningRequest()
                        let ctcp = CTCellularPlanProvisioning()
                        ctpr.address = smdpServerAddress
                        ctpr.matchingID = esimMatchingID
                        if #available(iOS 15.0, *) {
                            Task {
                                let result = await ctcp.addPlan(with: ctpr)
                                switch result{
                                case .unknown:
                                    let errorMessage = "{\"ErrorCode\" : 1, \"ErrorMessage\" : \"Unknown error\"}"
                                    sendPluginResult(status: CDVCommandStatus_ERROR, message: errorMessage)
                                case .fail:
                                    let errorMessage = "{\"ErrorCode\" : 2, \"ErrorMessage\" : \"Failed to Add eSIM\"}"
                                    sendPluginResult(status: CDVCommandStatus_ERROR, message: errorMessage)
                                case .success:
                                    sendPluginResult(status: CDVCommandStatus_OK, message: "OK, eSIM installed successfully")
                                @unknown default:
                                    let errorMessage = "{\"ErrorCode\" : 2, \"ErrorMessage\" : \"Failed to Add eSIM\"}"
                                    sendPluginResult(status: CDVCommandStatus_ERROR, message: errorMessage)
                                }
                            }
                        } else {
                            //iOS < 15
                            ctcp.addPlan(with: ctpr) { (result) in
                                switch result{
                                case .unknown:
                                    let errorMessage = "{\"ErrorCode\" : 1, \"ErrorMessage\" : \"Unknown error\"}"
                                    self.sendPluginResult(status: CDVCommandStatus_ERROR, message: errorMessage)
                                case .fail:
                                    let errorMessage = "{\"ErrorCode\" : 2, \"ErrorMessage\" : \"Failed to Add eSIM\"}"
                                    self.sendPluginResult(status: CDVCommandStatus_ERROR, message: errorMessage)
                                case .success:
                                    self.sendPluginResult(status: CDVCommandStatus_OK, message: "OK, eSIM installed successfully")
                                @unknown default:
                                    let errorMessage = "{\"ErrorCode\" : 2, \"ErrorMessage\" : \"Failed to Add eSIM\"}"
                                    self.sendPluginResult(status: CDVCommandStatus_ERROR, message: errorMessage)
                                }
                            }
                        }
                    } else {
                        //eSIM not supported
                        let errorMessage = "{\"ErrorCode\" : 3, \"ErrorMessage\" : \"This device is not supported\"}"
                        sendPluginResult(status: CDVCommandStatus_ERROR, message: errorMessage)
                    }
                } else {
                    //iOS < 12.0 (Not supported)
                    let errorMessage = "{\"ErrorCode\" : 4, \"ErrorMessage\" : \"Device not supported. iOS version should be 12.0 or higher\"}"
                    sendPluginResult(status: CDVCommandStatus_ERROR, message: errorMessage)
                }

            } else {
                //Missing input parameters
                let errorMessage = "{\"ErrorCode\" : 5, \"ErrorMessage\" : \"Missing input parameters\"}"
                sendPluginResult(status: CDVCommandStatus_ERROR, message: errorMessage)
            }
            
                    }   else {
            //Missing input parameters
            let errorMessage = "{\"ErrorCode\" : 6, \"ErrorMessage\" : \"Missing input parameters\"}"
            sendPluginResult(status: CDVCommandStatus_ERROR, message: errorMessage)
        }
    }
    
    func sendPluginResult(status: CDVCommandStatus, message: String) {
        pluginResult = CDVPluginResult(status: status, messageAs: message)
        self.commandDelegate!.send(pluginResult, callbackId: pluginCommand.callbackId)
    }
}
