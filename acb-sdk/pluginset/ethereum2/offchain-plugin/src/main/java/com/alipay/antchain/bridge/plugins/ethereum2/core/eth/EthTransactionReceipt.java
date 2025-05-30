/*
 * Copyright ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.alipay.antchain.bridge.plugins.ethereum2.core.eth;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import org.apache.tuweni.bytes.Bytes;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.datatypes.TransactionType;
import org.hyperledger.besu.ethereum.rlp.*;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.utils.Numeric;

/**
 * A transaction receipt, containing information pertaining a transaction execution.
 *
 * <p>Transaction receipts have two different formats: state root-encoded and status-encoded. The
 * difference between these two formats is that the state root-encoded transaction receipt contains
 * the state root for world state after the transaction has been processed (e.g. not invalid) and
 * the status-encoded transaction receipt instead has contains the status of the transaction (e.g. 1
 * for success and 0 for failure). The other transaction receipt fields are the same for both
 * formats: logs, logs bloom, and cumulative gas used in the block. The TransactionReceiptTypeEnum
 * attribute is the best way to check which format has been used.
 */
public class EthTransactionReceipt {

    private static final int NONEXISTENT = -1;

    public static EthTransactionReceipt generateFrom(TransactionReceipt receiptFromWeb3j) {
        return StrUtil.isEmpty(receiptFromWeb3j.getRoot()) ?
                new EthTransactionReceipt(
                        StrUtil.equals(receiptFromWeb3j.getType(), "0x0") ? TransactionType.FRONTIER : TransactionType.of(Numeric.decodeQuantity(receiptFromWeb3j.getType()).intValue()),
                        Numeric.decodeQuantity(receiptFromWeb3j.getStatus()).intValue(),
                        receiptFromWeb3j.getCumulativeGasUsed().longValue(),
                        receiptFromWeb3j.getLogs().stream()
                                .map(l -> new EthLog(Address.fromHexString(l.getAddress()), Bytes.fromHexString(l.getData()), l.getTopics().stream().map(EthLogTopic::fromHexString).toList()))
                                .toList(),
                        ObjectUtil.isNull(receiptFromWeb3j.getRevertReason()) ? Optional.empty() : Optional.of(Bytes.wrap(receiptFromWeb3j.getRevertReason().getBytes()))
                ) :
                new EthTransactionReceipt(
                        StrUtil.equals(receiptFromWeb3j.getType(), "0x0") ? TransactionType.FRONTIER : TransactionType.of(Numeric.decodeQuantity(receiptFromWeb3j.getType()).intValue()),
                        Hash.fromHexString(receiptFromWeb3j.getRoot()),
                        receiptFromWeb3j.getCumulativeGasUsed().longValue(),
                        receiptFromWeb3j.getLogs().stream()
                                .map(l -> new EthLog(Address.fromHexString(l.getAddress()), Bytes.fromHexString(l.getData()), l.getTopics().stream().map(EthLogTopic::fromHexString).toList()))
                                .toList(),
                        ObjectUtil.isNull(receiptFromWeb3j.getRevertReason()) ? Optional.empty() : Optional.of(Bytes.wrap(receiptFromWeb3j.getRevertReason().getBytes()))
                );
    }

    private final TransactionType transactionType;
    private final Hash stateRoot;
    private final long cumulativeGasUsed;
    private final List<EthLog> logs;
    private final LogsBloomFilter bloomFilter;
    private final int status;
    private final TransactionReceiptTypeEnum transactionReceiptType;
    private final Optional<Bytes> revertReason;

    /**
     * Creates an instance of a state root-encoded transaction receipt.
     *
     * @param stateRoot         the state root for the world state after the transaction has been processed
     * @param cumulativeGasUsed the total amount of gas consumed in the block after this transaction
     * @param logs              the logs generated within the transaction
     * @param revertReason      the revert reason for a failed transaction (if applicable)
     */
    public EthTransactionReceipt(
            final Hash stateRoot,
            final long cumulativeGasUsed,
            final List<EthLog> logs,
            final Optional<Bytes> revertReason) {
        this(
                TransactionType.FRONTIER,
                stateRoot,
                NONEXISTENT,
                cumulativeGasUsed,
                logs,
                LogsBloomFilter.builder().insertLogs(logs).build(),
                revertReason);
    }

    public EthTransactionReceipt(
            final TransactionType transactionType,
            final Hash stateRoot,
            final long cumulativeGasUsed,
            final List<EthLog> logs,
            final Optional<Bytes> revertReason) {
        this(
                transactionType,
                stateRoot,
                NONEXISTENT,
                cumulativeGasUsed,
                logs,
                LogsBloomFilter.builder().insertLogs(logs).build(),
                revertReason);
    }

    private EthTransactionReceipt(
            final TransactionType transactionType,
            final Hash stateRoot,
            final long cumulativeGasUsed,
            final List<EthLog> logs,
            final LogsBloomFilter bloomFilter,
            final Optional<Bytes> revertReason) {
        this(
                transactionType,
                stateRoot,
                NONEXISTENT,
                cumulativeGasUsed,
                logs,
                bloomFilter,
                revertReason);
    }

    /**
     * Creates an instance of a status-encoded transaction receipt.
     *
     * @param status            the status code for the transaction (1 for success and 0 for failure)
     * @param cumulativeGasUsed the total amount of gas consumed in the block after this transaction
     * @param logs              the logs generated within the transaction
     * @param revertReason      the revert reason for a failed transaction (if applicable)
     */
    public EthTransactionReceipt(
            final int status,
            final long cumulativeGasUsed,
            final List<EthLog> logs,
            final Optional<Bytes> revertReason) {
        this(
                TransactionType.FRONTIER,
                null,
                status,
                cumulativeGasUsed,
                logs,
                LogsBloomFilter.builder().insertLogs(logs).build(),
                revertReason);
    }

    public EthTransactionReceipt(
            final TransactionType transactionType,
            final int status,
            final long cumulativeGasUsed,
            final List<EthLog> logs,
            final LogsBloomFilter bloomFilter,
            final Optional<Bytes> revertReason) {
        this(transactionType, null, status, cumulativeGasUsed, logs, bloomFilter, revertReason);
    }

    public EthTransactionReceipt(
            final TransactionType transactionType,
            final int status,
            final long cumulativeGasUsed,
            final List<EthLog> logs,
            final Optional<Bytes> maybeRevertReason) {
        this(
                transactionType,
                status,
                cumulativeGasUsed,
                logs,
                LogsBloomFilter.builder().insertLogs(logs).build(),
                maybeRevertReason);
    }

    private EthTransactionReceipt(
            final TransactionType transactionType,
            final Hash stateRoot,
            final int status,
            final long cumulativeGasUsed,
            final List<EthLog> logs,
            final LogsBloomFilter bloomFilter,
            final Optional<Bytes> revertReason) {
        this.transactionType = transactionType;
        this.stateRoot = stateRoot;
        this.cumulativeGasUsed = cumulativeGasUsed;
        this.status = status;
        this.logs = logs;
        this.bloomFilter = bloomFilter;
        this.transactionReceiptType =
                stateRoot == null ? TransactionReceiptTypeEnum.STATUS : TransactionReceiptTypeEnum.ROOT;
        this.revertReason = revertReason;
    }

    /**
     * Write an RLP representation.
     *
     * @param out The RLP output to write to
     */
    public void writeToForNetwork(final RLPOutput out) {
        writeTo(out, false, false);
    }

    public void writeToForStorage(final RLPOutput out, final boolean compacted) {
        writeTo(out, true, compacted);
    }

    @VisibleForTesting
    void writeTo(final RLPOutput rlpOutput, final boolean withRevertReason, final boolean compacted) {
        if (transactionType.equals(TransactionType.FRONTIER)) {
            writeToForReceiptTrie(rlpOutput, withRevertReason, compacted);
        } else {
            rlpOutput.writeBytes(
                    RLP.encode(out -> writeToForReceiptTrie(out, withRevertReason, compacted)));
        }
    }

    public void writeToForReceiptTrie(
            final RLPOutput rlpOutput, final boolean withRevertReason, final boolean compacted) {
        if (!transactionType.equals(TransactionType.FRONTIER)) {
            rlpOutput.writeIntScalar(transactionType.getSerializedType());
        }

        rlpOutput.startList();

        // Determine whether it's a state root-encoded transaction receipt
        // or is a status code-encoded transaction receipt.
        if (stateRoot != null) {
            rlpOutput.writeBytes(stateRoot);
        } else {
            rlpOutput.writeLongScalar(status);
        }
        rlpOutput.writeLongScalar(cumulativeGasUsed);
        if (!compacted) {
            rlpOutput.writeBytes(bloomFilter);
        }
        rlpOutput.writeList(logs, (log, logOutput) -> log.writeTo(logOutput, compacted));
        if (withRevertReason && revertReason.isPresent()) {
            rlpOutput.writeBytes(revertReason.get());
        }
        rlpOutput.endList();
    }

    /**
     * Creates a transaction receipt for the given RLP
     *
     * @param input the RLP-encoded transaction receipt
     * @return the transaction receipt
     */
    public static EthTransactionReceipt readFrom(final RLPInput input) {
        return readFrom(input, true);
    }

    public static EthTransactionReceipt readFromTrieValue(final RLPInput input) {
        TransactionType transactionType = TransactionType.FRONTIER;
        if (!input.nextIsList()) {
            final Bytes typedTransactionReceiptBytes = input.readBytes();
            transactionType = TransactionType.of(typedTransactionReceiptBytes.get(0));
        }

        input.enterList();
        // Get the first element to check later to determine the
        // correct transaction receipt encoding to use.
        final RLPInput firstElement = input.readAsRlp();
        final long cumulativeGas = input.readLongScalar();

        LogsBloomFilter bloomFilter = null;

        final boolean hasLogs = !input.nextIsList() && input.nextSize() == LogsBloomFilter.BYTE_SIZE;
        if (hasLogs) {
            // The logs below will populate the bloom filter upon construction.
            bloomFilter = LogsBloomFilter.readFrom(input);
        }
        // TODO consider validating that the logs and bloom filter match.
        final boolean compacted = !hasLogs;
        final List<EthLog> logs = input.readList(logInput -> EthLog.readFrom(logInput, compacted));
        if (compacted) {
            bloomFilter = LogsBloomFilter.builder().insertLogs(logs).build();
        }

        // Status code-encoded transaction receipts have a single
        // byte for success (0x01) or failure (0x80).
        if (firstElement.raw().size() == 1) {
            final int status = firstElement.readIntScalar();
            input.leaveList();
            return new EthTransactionReceipt(
                    transactionType, status, cumulativeGas, logs, bloomFilter, Optional.empty());
        } else {
            final Hash stateRoot = Hash.wrap(firstElement.readBytes32());
            input.leaveList();
            return new EthTransactionReceipt(
                    transactionType, stateRoot, cumulativeGas, logs, bloomFilter, Optional.empty());
        }
    }


    /**
     * Creates a transaction receipt for the given RLP
     *
     * @param rlpInput            the RLP-encoded transaction receipt
     * @param revertReasonAllowed whether the rlp input is allowed to have a revert reason
     * @return the transaction receipt
     */
    public static EthTransactionReceipt readFrom(
            final RLPInput rlpInput, final boolean revertReasonAllowed) {
        RLPInput input = rlpInput;
        TransactionType transactionType = TransactionType.FRONTIER;
        if (!rlpInput.nextIsList()) {
            final Bytes typedTransactionReceiptBytes = input.readBytes();
            transactionType = TransactionType.of(typedTransactionReceiptBytes.get(0));
            input = new BytesValueRLPInput(typedTransactionReceiptBytes.slice(1), false);
        }

        input.enterList();
        // Get the first element to check later to determine the
        // correct transaction receipt encoding to use.
        final RLPInput firstElement = input.readAsRlp();
        final long cumulativeGas = input.readLongScalar();

        LogsBloomFilter bloomFilter = null;

        final boolean hasLogs = !input.nextIsList() && input.nextSize() == LogsBloomFilter.BYTE_SIZE;
        if (hasLogs) {
            // The logs below will populate the bloom filter upon construction.
            bloomFilter = LogsBloomFilter.readFrom(input);
        }
        // TODO consider validating that the logs and bloom filter match.
        final boolean compacted = !hasLogs;
        final List<EthLog> logs = input.readList(logInput -> EthLog.readFrom(logInput, compacted));
        if (compacted) {
            bloomFilter = LogsBloomFilter.builder().insertLogs(logs).build();
        }

        final Optional<Bytes> revertReason;
        if (input.isEndOfCurrentList()) {
            revertReason = Optional.empty();
        } else {
            if (!revertReasonAllowed) {
                throw new RLPException("Unexpected value at end of TransactionReceipt");
            }
            revertReason = Optional.of(input.readBytes());
        }

        // Status code-encoded transaction receipts have a single
        // byte for success (0x01) or failure (0x80).
        if (firstElement.raw().size() == 1) {
            final int status = firstElement.readIntScalar();
            input.leaveList();
            return new EthTransactionReceipt(
                    transactionType, status, cumulativeGas, logs, bloomFilter, revertReason);
        } else {
            final Hash stateRoot = Hash.wrap(firstElement.readBytes32());
            input.leaveList();
            return new EthTransactionReceipt(
                    transactionType, stateRoot, cumulativeGas, logs, bloomFilter, revertReason);
        }
    }

    /**
     * Returns the state root for a state root-encoded transaction receipt
     *
     * @return the state root if the transaction receipt is state root-encoded; otherwise {@code null}
     */
    public Hash getStateRoot() {
        return stateRoot;
    }

    /**
     * Returns the total amount of gas consumed in the block after the transaction has been processed.
     *
     * @return the total amount of gas consumed in the block after the transaction has been processed
     */
    public long getCumulativeGasUsed() {
        return cumulativeGasUsed;
    }

    /**
     * Returns the logs generated by the transaction.
     *
     * @return the logs generated by the transaction
     */
    public List<? extends EthLog> getLogs() {
        return logs;
    }

    /**
     * Returns the logs generated by the transaction.
     *
     * @return the logs generated by the transaction
     */
    public List<EthLog> getLogsList() {
        return logs;
    }

    /**
     * Returns the logs bloom filter for the logs generated by the transaction
     *
     * @return the logs bloom filter for the logs generated by the transaction
     */
    public LogsBloomFilter getBloomFilter() {
        return bloomFilter;
    }

    /**
     * Returns the status code for the status-encoded transaction receipt
     *
     * @return the status code if the transaction receipt is status-encoded; otherwise {@code -1}
     */
    public int getStatus() {
        return status;
    }

    public TransactionReceiptTypeEnum getTransactionReceiptType() {
        return transactionReceiptType;
    }

    public Optional<Bytes> getRevertReason() {
        return revertReason;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof EthTransactionReceipt)) {
            return false;
        }
        final EthTransactionReceipt other = (EthTransactionReceipt) obj;
        return logs.equals(other.getLogsList())
               && Objects.equals(stateRoot, other.stateRoot)
               && cumulativeGasUsed == other.getCumulativeGasUsed()
               && status == other.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(logs, stateRoot, cumulativeGasUsed);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("stateRoot", stateRoot)
                .add("cumulativeGasUsed", cumulativeGasUsed)
                .add("logs", logs)
                .add("bloomFilter", bloomFilter)
                .add("status", status)
                .add("transactionReceiptType", transactionReceiptType)
                .toString();
    }
}
