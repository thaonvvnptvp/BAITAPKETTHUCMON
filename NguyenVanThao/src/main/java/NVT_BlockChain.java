import java.security.Security;
import java.util.ArrayList;
import java.util.HashMap;
//import java.util.Map;
import java.util.Scanner;

public class NVT_BlockChain {

    public static ArrayList<VNPT_THAO> blockchain = new ArrayList<VNPT_THAO>();
    public static HashMap<String,TransactionOutput> UTXOs = new HashMap<String,TransactionOutput>();
    public static int difficulty = 3;
    public static float minimumTransaction = 0.1f;
    public static Store kho1; //Kho 1
    public static Store kho2; //Kho 2
    public static Transaction genesisTransaction;

    public static void main(String[] args) {
        //add our blocks to the blockchain ArrayList:
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider()); //Thiết lập bảo mật bằng phương thức BouncyCastleProvider
        Scanner sc = new Scanner(System.in);

        //Create Mobiles in Store
        kho1 = new Store();
        kho2 = new Store();
        Store coinbase = new Store();
        Transaction sendFund;
            // Nhap thong tin khoi tao kho 1
            System.out.print("Nhap so luong VNPT-Net Router trong kho 1: ");
            float initBalanceA = sc.nextFloat();

            //Khởi tạo giao dịch gốc, để chuyển VNPT-Net Router vào kho 1
            genesisTransaction = new Transaction(coinbase.publicKey, kho1.publicKey, initBalanceA, null);
            genesisTransaction.generateSignature(coinbase.privateKey);     //Gán private key (ký thủ công) vào giao dịch gốc
            genesisTransaction.transactionId = "0"; //Gán ID cho giao dịch gốc
            genesisTransaction.outputs.add(new TransactionOutput(genesisTransaction.reciepient, genesisTransaction.value, genesisTransaction.transactionId)); //Thêm Transactions Output
            UTXOs.put(genesisTransaction.outputs.get(0).id, genesisTransaction.outputs.get(0)); //Lưu giao dịch đầu tiên vào danh sách UTXOs.

            VNPT_THAO genesis = new VNPT_THAO("0");
            genesis.addTransaction(genesisTransaction);
            addBlock(genesis);
            System.out.println("So VNPT-Net Router trong kho 1 la : " + kho1.getBalance());

            // Nhap thong tin khoi tao kho 2
            System.out.print("Nhap so luong VNPT-Net Router trong kho 2: ");
            float initBalanceB = sc.nextFloat();

            //Khởi tạo giao dịch gốc, để chuyển VNPT-Net Router vào kho 2
            genesisTransaction = new Transaction(coinbase.publicKey, kho2.publicKey, initBalanceB, null);
            genesisTransaction.generateSignature(coinbase.privateKey);     //Gán private key (ký thủ công) vào giao dịch gốc
            genesisTransaction.transactionId = "0"; //Gán ID cho giao dịch gốc
            genesisTransaction.outputs.add(new TransactionOutput(genesisTransaction.reciepient, genesisTransaction.value, genesisTransaction.transactionId)); //Thêm Transactions Output
            UTXOs.put(genesisTransaction.outputs.get(0).id, genesisTransaction.outputs.get(0)); //Lưu giao dịch đầu tiên vào danh sách UTXOs.
            genesis.addTransaction(genesisTransaction);
            addBlock(genesis);
            System.out.println("So VNPT-Net Router của kho 2 la : " + kho2.getBalance());

            VNPT_THAO block1 = new VNPT_THAO(genesis.hash);

            //Kiểm tra số lượng chuyển thoả mãn yêu cầu
            boolean fail = true;
            while (fail){
                System.out.print("Nhap so luong VNPT-Net Router can chuyen tu kho 1 den kho 2: ");
                float numberTransfer = sc.nextFloat();
                System.out.println("Dang xu ly .............................................");
                sendFund = kho1.sendFunds(kho2.publicKey, numberTransfer);
                if (sendFund==null){
                    continue;
                }else{
                    fail= false;
                    block1.addTransaction(sendFund);
                }
            }
            addBlock(block1);
            System.out.println("Ket qua so VNPT-Net Router moi trong cac kho sau khi chuyen: ");
            System.out.println("So VNPT-Net Router trong kho 1 la : " + kho1.getBalance());
            System.out.println("So VNPT-Net Router trong kho 2 la : " + kho2.getBalance());
    }

    public static Boolean isChainValid() {
        VNPT_THAO currentBlock;
        VNPT_THAO previousBlock;
        String hashTarget = new String(new char[difficulty]).replace('\0', '0');
        HashMap<String,TransactionOutput> tempUTXOs = new HashMap<String,TransactionOutput>(); //Tạo một danh sách hoạt động tạm thời của các giao dịch chưa được thực thi tại một trạng thái khối nhất định.
        tempUTXOs.put(genesisTransaction.outputs.get(0).id, genesisTransaction.outputs.get(0));

        //loop through blockchain to check hashes:
        for(int i=1; i < blockchain.size(); i++) {

            currentBlock = blockchain.get(i);
            previousBlock = blockchain.get(i-1);
            //Kiểm tra, so sánh mã băm đã đăng ký với mã băm được tính toán
            if(!currentBlock.hash.equals(currentBlock.calculateHash()) ){
                System.out.println("#Mã băm khối hiện tại không khớp");
                return false;
            }
            //So sánh mã băm của khối trước với mã băm của khối trước đã được đăng ký
            if(!previousBlock.hash.equals(currentBlock.previousHash) ) {
                System.out.println("#Mã băm khối trước không khớp");
                return false;
            }
            //Kiểm tra xem mã băm có lỗi không
            if(!currentBlock.hash.substring( 0, difficulty).equals(hashTarget)) {
                System.out.println("#Khối này không đào được do lỗi!");
                return false;
            }

            //Vòng lặp kiểm tra các giao dịch
            TransactionOutput tempOutput;
            for(int t=0; t <currentBlock.transactions.size(); t++) {
                Transaction currentTransaction = currentBlock.transactions.get(t);

                if(!currentTransaction.verifySignature()) {
                    System.out.println("#Chữ ký số của giao dịch (" + t + ") không hợp lệ");
                    return false;
                }
                if(currentTransaction.getInputsValue() != currentTransaction.getOutputsValue()) {
                    System.out.println("#Các đầu vào không khớp với đầu ra trong giao dịch (" + t + ")");
                    return false;
                }

                for(TransactionInput input: currentTransaction.inputs) {
                    tempOutput = tempUTXOs.get(input.transactionOutputId);

                    if(tempOutput == null) {
                        System.out.println("#Các đầu vào tham chiếu trong giao dịch (" + t + ") bị thiếu!");
                        return false;
                    }

                    if(input.UTXO.value != tempOutput.value) {
                        System.out.println("#Các đầu vào tham chiếu trong giao dịch (" + t + ") có giá trị không hợp lệ");
                        return false;
                    }

                    tempUTXOs.remove(input.transactionOutputId);
                }

                for(TransactionOutput output: currentTransaction.outputs) {
                    tempUTXOs.put(output.id, output);
                }

                if( currentTransaction.outputs.get(0).reciepient != currentTransaction.reciepient) {
                    System.out.println("#Giao dịch(" + t + ") có người nhận không đúng!");
                    return false;
                }
                if( currentTransaction.outputs.get(1).reciepient != currentTransaction.sender) {
                    System.out.println("#Đầu ra của giao (" + t + ") không đúng với người gửi.");
                    return false;
                }

            }

        }
        System.out.println("Chuỗi khối hợp lệ!");
        return true;
    }

    public static void addBlock(VNPT_THAO newBlock) {
        newBlock.mineBlock(difficulty);
        blockchain.add(newBlock);
    }
}

