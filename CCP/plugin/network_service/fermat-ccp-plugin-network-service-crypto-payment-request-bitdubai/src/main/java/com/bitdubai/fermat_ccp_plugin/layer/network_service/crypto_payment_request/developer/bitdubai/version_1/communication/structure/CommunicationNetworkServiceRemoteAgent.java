package com.bitdubai.fermat_ccp_plugin.layer.network_service.crypto_payment_request.developer.bitdubai.version_1.communication.structure;


import com.bitdubai.fermat_api.layer.all_definition.crypto.asymmetric.AsymmectricCryptography;
import com.bitdubai.fermat_api.layer.all_definition.crypto.asymmetric.ECCKeyPair;
import com.bitdubai.fermat_api.layer.all_definition.enums.Plugins;
import com.bitdubai.fermat_api.layer.all_definition.events.interfaces.FermatEvent;
import com.bitdubai.fermat_ccp_plugin.layer.network_service.crypto_payment_request.developer.bitdubai.version_1.CryptoPaymentRequestNetworkServicePluginRoot;
import com.bitdubai.fermat_ccp_plugin.layer.network_service.crypto_payment_request.developer.bitdubai.version_1.communication.database.CommunicationLayerNetworkServiceDatabaseConstants;
import com.bitdubai.fermat_ccp_plugin.layer.network_service.crypto_payment_request.developer.bitdubai.version_1.communication.database.IncomingMessageDao;
import com.bitdubai.fermat_ccp_plugin.layer.network_service.crypto_payment_request.developer.bitdubai.version_1.communication.database.OutgoingMessageDao;
import com.bitdubai.fermat_ccp_plugin.layer.network_service.crypto_payment_request.developer.bitdubai.version_1.communication.exceptions.CantInsertRecordDataBaseException;
import com.bitdubai.fermat_ccp_plugin.layer.network_service.crypto_payment_request.developer.bitdubai.version_1.communication.exceptions.CantReadRecordDataBaseException;
import com.bitdubai.fermat_ccp_plugin.layer.network_service.crypto_payment_request.developer.bitdubai.version_1.communication.exceptions.CantUpdateRecordDataBaseException;
import com.bitdubai.fermat_p2p_api.layer.all_definition.communication.commons.contents.FermatMessageCommunication;
import com.bitdubai.fermat_p2p_api.layer.all_definition.communication.enums.EventType;
import com.bitdubai.fermat_p2p_api.layer.p2p_communication.MessagesStatus;
import com.bitdubai.fermat_p2p_api.layer.p2p_communication.commons.client.CommunicationsVPNConnection;
import com.bitdubai.fermat_p2p_api.layer.p2p_communication.commons.contents.FermatMessage;
import com.bitdubai.fermat_p2p_api.layer.p2p_communication.commons.enums.FermatMessagesStatus;
import com.bitdubai.fermat_pip_api.layer.pip_platform_service.error_manager.ErrorManager;
import com.bitdubai.fermat_pip_api.layer.pip_platform_service.error_manager.UnexpectedPluginExceptionSeverity;
import com.bitdubai.fermat_p2p_api.layer.all_definition.communication.events.NewNetworkServiceMessageSentNotificationEvent;
import com.bitdubai.fermat_pip_api.layer.pip_platform_service.event_manager.interfaces.EventManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;

/**
 * The Class <code>com.bitdubai.fermat_ccp_plugin.layer.network_service.crypto_payment_request.developer.bitdubai.version_1.communication.structure.CommunicationNetworkServiceRemoteAgent</code>
 * is the service toRead that maintaining the communication channel, read and wait for new message.
 *
 * This class extend of the <code>java.util.Observable</code> class,  its used on the software design pattern called: The observer pattern,
 * for more info see @link https://en.wikipedia.org/wiki/Observer_pattern
 *
 * <p/>
 *
 * Created by Leon Acosta - (laion.cj91@gmail.com) on 06/10/2015.
 *
 * @version 1.0
 * @since Java JDK 1.7
 */
public class CommunicationNetworkServiceRemoteAgent extends Observable {

    /*
     * Represent the sleep time for the read or send (2000 milliseconds)
     */
    private static final long SLEEP_TIME = 2000;


    private final CommunicationsVPNConnection communicationsVPNConnection  ;
    private final ErrorManager                errorManager                 ;
    private final EventManager                eventManager                 ;
    private final IncomingMessageDao          incomingMessageDao           ;
    private final OutgoingMessageDao          outgoingMessageDao           ;
    private final ECCKeyPair                  eccKeyPair                   ;
    private final String                      remoteNetworkServicePublicKey;

    /**
     * Represent is the tread is running
     */
    private Boolean running;

    /**
     * Represent the read messages tread of this CommunicationNetworkServiceRemoteAgent
     */
    private Thread toReceive;

    /**
     * Represent the send messages tread of this CommunicationNetworkServiceRemoteAgent
     */
    private Thread toSend;


    /**
     * Constructor with parameters.
     */
    public CommunicationNetworkServiceRemoteAgent(final ECCKeyPair                  eccKeyPair                   ,
                                                  final CommunicationsVPNConnection communicationsVPNConnection  ,
                                                  final String                      remoteNetworkServicePublicKey,
                                                  final ErrorManager                errorManager                 ,
                                                  final EventManager                eventManager                 ,
                                                  final IncomingMessageDao          incomingMessageDao           ,
                                                  final OutgoingMessageDao          outgoingMessageDao           ) {

        super();
        this.eccKeyPair                          = eccKeyPair;
        this.remoteNetworkServicePublicKey       = remoteNetworkServicePublicKey;
        this.errorManager                        = errorManager;
        this.eventManager                        = eventManager;
        this.running                             = Boolean.FALSE;
        this.incomingMessageDao                  = incomingMessageDao;
        this.outgoingMessageDao                  = outgoingMessageDao;
        this.communicationsVPNConnection         = communicationsVPNConnection;


        //Create a thread to receive the messages
        this.toReceive = new Thread(new Runnable() {
            @Override
            public void run() {
                while (running)
                    processMessageReceived();
            }
        });

        //Create a thread to send the messages
        this.toSend = new Thread(new Runnable() {
            @Override
            public void run() {
                while (running)
                    processMessageToSend();
            }
        });

    }

    /**
     * Start the internal threads to make the job
     */
    public void start(){

        //Set to running
        this.running  = Boolean.TRUE;

        //Start the Threads
        toReceive.start();
        toSend.start();

        System.out.println("CommunicationNetworkServiceRemoteAgent - started ");

    }

    /**
     * Pause the internal threads
     */
    public void pause(){
        this.running  = Boolean.FALSE;
    }

    /**
     * Resume the internal threads
     */
    public void resume(){
        this.running  = Boolean.TRUE;
    }

    /**
     * Stop the internal threads
     */
    public void stop(){

        //Stop the Threads
        toReceive.interrupt();
        toSend.interrupt();

        //Disconnect from the service
        communicationsVPNConnection.close();
    }

    /**
     * This method process the message received and save on the
     * data base in the table <code>incoming_messages</code> and notify all observers
     * to the new messages received
     */
    private void processMessageReceived(){

        try {

            System.out.println("CommunicationNetworkServiceRemoteAgent - "+communicationsVPNConnection.isActive());

            /**
             * Verified the status of the connection
             */
            if (communicationsVPNConnection.isActive()){

                System.out.println("CommunicationNetworkServiceRemoteAgent - "+communicationsVPNConnection.getUnreadMessagesCount());

                /**
                 * process all pending messages
                 */
                for (int i = 0; i < communicationsVPNConnection.getUnreadMessagesCount(); i++) {

                    /*
                     * Read the next message in the queue
                     */
                    FermatMessage message = communicationsVPNConnection.readNextMessage();


                    /*
                     * Validate the message signature
                     */
                    AsymmectricCryptography.verifyMessageSignature(message.getSignature(), message.getContent(), remoteNetworkServicePublicKey);

                    /*
                     * Decrypt the message content
                     */
                    ((FermatMessageCommunication) message).setContent(AsymmectricCryptography.decryptMessagePrivateKey(message.getContent(), eccKeyPair.getPrivateKey()));

                    /*
                     * Change to the new status
                     */
                    ((FermatMessageCommunication) message).setFermatMessagesStatus(FermatMessagesStatus.NEW_RECEIVED);

                    /*
                     * Save to the data base table
                     */
                    incomingMessageDao.create(message);

                    /*
                     * Remove the message from the queue
                     */
                    communicationsVPNConnection.removeMessageRead(message);

                    /*
                     * Notify all observer of this agent that Received a new message
                     */
                    setChanged();
                    notifyObservers(message);

                }

            }

            //Sleep for a time
            toReceive.sleep(CommunicationNetworkServiceRemoteAgent.SLEEP_TIME);

        } catch (InterruptedException e) {
            errorManager.reportUnexpectedPluginException(Plugins.BITDUBAI_TEMPLATE_NETWORK_SERVICE, UnexpectedPluginExceptionSeverity.DISABLES_SOME_FUNCTIONALITY_WITHIN_THIS_PLUGIN, new Exception("Can not sleep"));
        } catch (CantInsertRecordDataBaseException e) {
            errorManager.reportUnexpectedPluginException(Plugins.BITDUBAI_TEMPLATE_NETWORK_SERVICE, UnexpectedPluginExceptionSeverity.DISABLES_SOME_FUNCTIONALITY_WITHIN_THIS_PLUGIN, new Exception("Can not process message received. Error reason: "+e.getMessage()));
        }

    }

    /**
     * This method read for new messages pending to send on the data base in
     * the table <code>outbox_messages</code> and encrypt the message content,
     * sing the message and send it
     */
    public void processMessageToSend(){

        try {

                try {

                    Map<String, Object> filters = new HashMap<>();
                    filters.put(CommunicationLayerNetworkServiceDatabaseConstants.OUTGOING_MESSAGES_STATUS_COLUMN_NAME, MessagesStatus.PENDING_TO_SEND.getCode());
                    filters.put(CommunicationLayerNetworkServiceDatabaseConstants.OUTGOING_MESSAGES_RECEIVER_ID_COLUMN_NAME, remoteNetworkServicePublicKey);

                    /*
                     * Read all pending message from database
                     */
                    List<FermatMessage> messages = outgoingMessageDao.findAll(filters);
                    /*
                     * For each message
                     */
                    for (FermatMessage message: messages){


                        if (communicationsVPNConnection.isActive() && (message.getFermatMessagesStatus() != FermatMessagesStatus.SENT)) {

                            /*
                             * Encrypt the content of the message whit the remote public key
                             */
                            ((FermatMessageCommunication) message).setContent(AsymmectricCryptography.encryptMessagePublicKey(message.getContent(), remoteNetworkServicePublicKey));

                            /*
                             * Sing the message
                             */
                            String signature = AsymmectricCryptography.createMessageSignature(message.getContent(), eccKeyPair.getPrivateKey());
                            ((FermatMessageCommunication) message).setSignature(signature);

                            /*
                             * Send the message
                             */
                            communicationsVPNConnection.sendMessage(message);

                            /*
                             * Change the message and update in the data base
                             */
                            ((FermatMessageCommunication) message).setFermatMessagesStatus(FermatMessagesStatus.SENT);
                            outgoingMessageDao.update(message);

                            /*
                             * Put the message on a event and fire new event
                             */
                            FermatEvent fermatEvent = eventManager.getNewEvent(EventType.NEW_NETWORK_SERVICE_MESSAGE_SENT_NOTIFICATION);
                            fermatEvent.setSource(CryptoPaymentRequestNetworkServicePluginRoot.EVENT_SOURCE);
                            ((NewNetworkServiceMessageSentNotificationEvent) fermatEvent).setData(message);
                            eventManager.raiseEvent(fermatEvent);

                        }
                    }

                } catch (CantUpdateRecordDataBaseException e) {
                    errorManager.reportUnexpectedPluginException(Plugins.BITDUBAI_TEMPLATE_NETWORK_SERVICE, UnexpectedPluginExceptionSeverity.DISABLES_SOME_FUNCTIONALITY_WITHIN_THIS_PLUGIN, new Exception("Can not process messages to send. Error reason: "+e.getMessage()));
                } catch (CantReadRecordDataBaseException e) {
                    errorManager.reportUnexpectedPluginException(Plugins.BITDUBAI_TEMPLATE_NETWORK_SERVICE, UnexpectedPluginExceptionSeverity.DISABLES_SOME_FUNCTIONALITY_WITHIN_THIS_PLUGIN, new Exception("Can not process messages to send. Error reason: " + e.getMessage()));
                }

            //Sleep for a time
            toSend.sleep(CommunicationNetworkServiceRemoteAgent.SLEEP_TIME);

        } catch (InterruptedException e) {
            errorManager.reportUnexpectedPluginException(Plugins.BITDUBAI_TEMPLATE_NETWORK_SERVICE, UnexpectedPluginExceptionSeverity.DISABLES_SOME_FUNCTIONALITY_WITHIN_THIS_PLUGIN, new Exception("Can not sleep"));
        }

    }

}
