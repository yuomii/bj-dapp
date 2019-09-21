package app.util;

import app.App;
import crypto.CPXKey;
import crypto.CryptoService;
import message.request.cmd.GetAccountCmd;
import message.request.cmd.GetContractByIdCmd;
import message.request.cmd.SendRawTransactionCmd;
import message.response.ExecResult;
import message.transaction.FixedNumber;
import message.transaction.Transaction;
import message.transaction.TransactionType;
import message.util.GenericJacksonWriter;
import message.util.RequestCallerService;
import org.ethereum.solidity.Abi;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.security.interfaces.ECPrivateKey;
import java.time.Instant;
import java.util.HashMap;

@Service
public class TransactionUtil {

    private GenericJacksonWriter writer = new GenericJacksonWriter();
    private RequestCallerService caller = new RequestCallerService();
    private CryptoService crypto = new CryptoService();

    public HashMap<String, Object> executeMethod(ECPrivateKey privateKey, long nonce , int abiIndex, double amount) {
        try {
            final byte[] payContractSignature = Abi.fromJson(App.getContractAbi()).get(abiIndex).fingerprintSignature();
            final HashMap<String, Object> resultMap = executeTransaction(privateKey, nonce, TransactionType.CALL, amount, payContractSignature);
            final String txId = (String) resultMap.get("txId");
            final GetContractByIdCmd cmd = new GetContractByIdCmd(txId);
            final String response = caller.postRequest(App.getRpcUrl(), cmd);
            return writer.getObjectFromString(ExecResult.class, response).getResult();
        } catch (Exception e){
            return new HashMap<>();
        }
    }

    public HashMap<String, Object> executeMethodWithParameters(ECPrivateKey privateKey, long nonce , int abiIndex, double amount, double param) {
        try {
            Abi.Function c = (Abi.Function) Abi.fromJson(App.getContractAbi()).get(abiIndex);
            byte [] params = c.encode(new FixedNumber(param).getValue());
            final HashMap<String, Object> resultMap = executeTransaction(privateKey, nonce, TransactionType.CALL, amount, params);
            final String txId = (String) resultMap.get("txId");
            final GetContractByIdCmd cmd = new GetContractByIdCmd(txId);
            final String response = caller.postRequest(App.getRpcUrl(), cmd);
            return writer.getObjectFromString(ExecResult.class, response).getResult();
        } catch (Exception e){
            return new HashMap<>();
        }
    }

    public HashMap<String, Object> getAccountBalance(String address){
        try{
            final GetAccountCmd cmd = new GetAccountCmd(address);
            final String response = caller.postRequest(App.getRpcUrl(), cmd);
            return writer.getObjectFromString(ExecResult.class, response).getResult();
        } catch (Exception e){
            return new HashMap<>();
        }
    }

    public HashMap<String, Object> executeTransaction(ECPrivateKey privateKey, long nonce, byte txType, double amount, byte [] data) {
        try {
            final Transaction tx = Transaction.builder()
                    .txType(txType)
                    .fromPubKeyHash(CPXKey.getScriptHash(privateKey))
                    .toPubKeyHash(CPXKey.getScriptHashFromCPXAddress(App.getGameAddress()))
                    .amount(new FixedNumber(amount))
                    .nonce(nonce)
                    .data(data)
                    .gasPrice(new FixedNumber(0.0000000001))
                    .gasLimit(BigInteger.valueOf(300000L))
                    .version(1)
                    .executeTime(Instant.now().toEpochMilli())
                    .build();
            final SendRawTransactionCmd cmd = new SendRawTransactionCmd(tx.getBytes(crypto, privateKey));
            final String response = caller.postRequest(App.getRpcUrl(), cmd);
            return writer.getObjectFromString(ExecResult.class, response).getResult();
        } catch (Exception e){
            return new HashMap<>();
        }
    }

    public String cleanTextContent(String text) {
        // strips off all non-ASCII characters
        text = text.replaceAll("[^\\x00-\\x7F]", "");
        // erases all the ASCII control characters
        text = text.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");
        // removes non-printable characters from Unicode
        text = text.replaceAll("\\p{C}", "");
        return text.trim();
    }

}