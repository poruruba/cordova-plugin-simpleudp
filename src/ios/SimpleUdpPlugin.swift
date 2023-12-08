import Foundation
import Network

@objc(SimpleUdpPlugin)
class SimpleUdpPlugin : CDVPlugin
{
    var sockets:[UInt16:NWListener] = [:]
    
	override
    func pluginInitialize() {
    }
    
    @objc(receiving:)
    func receiving(command: CDVInvokedUrlCommand){
        // port
        NSLog("receiving called")
        guard let port = command.arguments[0] as? UInt16 else {
            let pluginResult:CDVPluginResult = CDVPluginResult(status:CDVCommandStatus_ERROR, messageAs: "Parameter invalid")
            self.commandDelegate.send(pluginResult, callbackId: command.callbackId)
            return
        }

        do {
            if let listener = self.sockets[port] {
                listener.cancel()
            }
            let localPort = NWEndpoint.Port(rawValue: port)
            let listener = try NWListener(using: .udp, on: localPort!)
            listener.stateUpdateHandler = { state in
                switch state {
                case .ready:
                    print("listener ready")
                case .failed(let error):
                    print("listener2 error：　\(error)")
                    let pluginResult:CDVPluginResult = CDVPluginResult(status:CDVCommandStatus_ERROR, messageAs: "listener failed")
                    self.commandDelegate.send(pluginResult, callbackId: command.callbackId)
                    listener.cancel()
                default:
                    break
                }
            }
            
            listener.newConnectionHandler = { newConnection in
                newConnection.start(queue: .global())
                newConnection.receiveMessage { (data, context, isComplete, error ) in
                    if let data = data {
                        if let receivedString = String(data: data, encoding: .utf8){
                            print("receive message:　\(receivedString)")
                            newConnection.forceCancel()

                            let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: ["payload": receivedString])
                            pluginResult?.keepCallback = true
                            self.commandDelegate.send(pluginResult, callbackId: command.callbackId)
                        }else if let error = error {
                            print("receive error: \(error)")
                        }
                    }
                }
            }
            listener.start(queue: .global())
            self.sockets[port] = listener
        }catch{
            let pluginResult:CDVPluginResult = CDVPluginResult(status:CDVCommandStatus_ERROR, messageAs: "unknown")
            self.commandDelegate.send(pluginResult, callbackId: command.callbackId)
        }

    }

    @objc(send:)
    func send(command: CDVInvokedUrlCommand)
    {
        // message, host, port
        NSLog("send called")
        guard let message = command.arguments[0] as? String, let host = command.arguments[1] as? String, let port = command.arguments[2] as? UInt16 else {
            NSLog("Parameter invalid")
            let pluginResult:CDVPluginResult = CDVPluginResult(status:CDVCommandStatus_ERROR, messageAs: "Parameter invalid")
            self.commandDelegate.send(pluginResult, callbackId:command.callbackId)
            return
        }
                    
        let sendPort = NWEndpoint.Port(rawValue: port)
        let sendHost = NWEndpoint.Host(host)
        let connection = NWConnection(host: sendHost, port: sendPort!, using: .udp)
        connection.start(queue: .global())
        if let messageData = message.data(using: .utf8){
            connection.send(content: messageData, completion: .contentProcessed{ error in
                if let error = error {
                    print("send error: \(error)")
                    let pluginResult:CDVPluginResult = CDVPluginResult(status:CDVCommandStatus_ERROR, messageAs: "send falied")
                    self.commandDelegate.send(pluginResult, callbackId: command.callbackId)
                }else{
                    print("send success")
                    let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: "OK")
                    self.commandDelegate.send(pluginResult, callbackId: command.callbackId)
                }
                connection.cancel()
            })
        }
    }
 }
