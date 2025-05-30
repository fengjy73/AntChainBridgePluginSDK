// SPDX-License-Identifier: Apache-2.0
pragma solidity ^0.8.0;

interface ISubProtocol {

    /**
    * @dev AM contract that want to forward the message to the receiving blockchain need to call this method to send auth message.
     *
     * @param senderDomain the domain name of the sending blockchain.
     * @param senderID the identity of the sender.
     * @param pkg the raw message from AM contract
     */
    function recvMessage(string calldata senderDomain, bytes32 senderID, bytes calldata pkg) external;

    /**
    * @dev SDP contract are based on the AuthMessage contract. Here we set the AuthMessage contract identity.
     *
     * @param newAmContract the identity of the AuthMessage contract.
     */
    function setAmContract(identity newAmContract) external;
}
