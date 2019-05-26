import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import com.ibm.jzos.FileFactory;


class Manager{
    public int numberEmpty;
    public int numberPart;
    public int numberOpen;
    public int totalSum;
    public boolean log;
    public ArrayList<String> errors;
    public ArrayList<String> numbersPay;
    public ArrayList<String> numbersInv;
    public BufferedWriter writer;
    public Manager(boolean l, String fileOut){
        errors=new ArrayList<>();
        numbersPay=new ArrayList<>();
        numbersInv=new ArrayList<>();
        log=l;
        writer=FileFactory.newBufferedReader(fileOut);
    }
    public ArrayList<String> readFile(String name,String type) throws IOException, InterruptedException {
        BufferedReader br = FileFactory.newBufferedReader(name);
        ArrayList<String> result=new ArrayList<>();
        String st;
        while ((st = br.readLine()) != null) {
            Date date=new Date();
            SimpleDateFormat formatForDateNow = new SimpleDateFormat("yyyy.MM.dd hh:mm:ss");
            boolean cor=true;
            if("inv".equals(type)){
                cor=this.checkCorrectInv(st);
            }
            else{
                cor=this.checkCorrectPay(st);
            }
            if(!cor){
                st=("inv".equals(type)?"I ":"P ")+st;
                errors.add(st);
            }
            else{
                result.add(st+" "+formatForDateNow.format(date));
            }
        }
        br.close();
        return result;
    }
    public boolean checkCorrectInv(String st){
        String[] all=st.split(" ");
        if(all.length!=2) return false;
        try{
            int temp=Integer.parseInt(all[1]);
        }
        catch(NumberFormatException e){
            return false;
        }
        if(this.numbersInv.indexOf(all[0])==-1){
            this.numbersInv.add(all[0]);
        }
        else{
            return false;
        }
        return true;
    }
    public boolean checkCorrectPay(String st){
        String[] all=st.split(" ");
        if(all.length!=3) return false;
        try{
            int temp=Integer.parseInt(all[1]);
        }
        catch(NumberFormatException e){
            return false;
        }
        if(this.numbersPay.indexOf(all[0])==-1){
            this.numbersPay.add(all[0]);
        }
        else{
            return false;
        }
        return true;
    }
    public void writeMaster(String inv,ArrayList<String> pays) throws IOException {
        String[] invs=inv.split(" ");
        int sum=Integer.parseInt(invs[1]);
        int curSum=this.getCurrentBalance(sum,pays);
        if(this.log){
            this.changeStat(sum,curSum);
        }
        String out="I \t"+invs[0]+" \t"+invs[2]+" \t"+sum+" \t"+curSum;
        writer.write(out);
        writer.write("\n");
        for (int j = 0; j < pays.size(); j++) {
            String[] temp=pays.get(j).split(" ");
            sum-=Integer.parseInt(temp[1]);
            out="P \t"+temp[0]+" \t"+temp[3]+" \t"+temp[1]+" \t"+sum;
            writer.write(out);
            writer.write("\n");
        }
        writer.write("----------------------\n");
    }
    public void writeMasterJustInv(ArrayList<String> invs) throws IOException {
        writer.write("Just invoices\n");
        for (int i = 0; i < invs.size(); i++) {
            String[] inv=invs.get(i).split(" ");
            this.changeStat(Integer.parseInt(inv[1]),Integer.parseInt(inv[1]));
            String out="I \t"+inv[0]+" \t"+inv[2]+" \t"+inv[1]+" \t"+inv[1];
            writer.write(out);
            writer.write("\n");
        }
        writer.write("----------------------\n");

    }
    public void writeMasterErrors() throws IOException {
        if(this.errors.size()!=0){
            writer.write("Invoices/Payments with errors:\n");
            for (int i = 0; i < errors.size(); i++) {
                String s =  errors.get(i);

                writer.write("Error in string "+s+"\n");
            }
            writer.write("----------------------\n");
        }

    }
    public void writeMasterStat() throws IOException {
        if(this.log){
            String inf="Number of settled invoices = "+this.numberEmpty+
                    "\nNumber of partially settled invoices = "+this.numberPart+
                    "\nNumber of open invoices = "+this.numberOpen+
                    "\nTotal amount = "+this.totalSum+"\n";
            writer.write(inf);
        }
    }
    private int getCurrentBalance(int sum,ArrayList<String> pays){
        for (int i = 0; i < pays.size(); i++) {
            Integer pay=Integer.parseInt(pays.get(i).split(" ")[1]);
            sum-=pay;
        }
        return sum;
    }
    public void changeStat(int sum,int cur){
        if(cur==0){
            this.numberEmpty++;
        }
        else{
            if(sum==cur){
                this.numberOpen++;
            }
            else{
                this.numberPart++;
            }
        }
        totalSum+=cur;
    }
    public void closeWriter() throws IOException {
        writer.close();
    }
}
public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        boolean log=false;
        if (args.length!=0&&"-log".equals(args[0])){
            log=true;
        }
        Manager m=new Manager(log,"//MASTER.TXT");
        ArrayList<String> invoices=m.readFile("//INVOICE.TXT","inv");
        ArrayList<String> payments=m.readFile("//PAYMENT.TXT","pay");
        ArrayList<String> invWithoutPay=new ArrayList<>();
        for (int i = 0; i < invoices.size(); i++) {
            String inv=invoices.get(i);
            String[] invs=inv.split(" ");
            boolean flag=false;
            ArrayList<String> payForInv=new ArrayList<>();
            for (int j = 0; j < payments.size(); j++) {
                String pay=payments.get(j);
                String[] pays=pay.split(" ");
                if(invs[0].equals(pays[2])){
                    flag=true;
                    payForInv.add(pay);
                }
            }
            if(flag){
                m.writeMaster(inv,payForInv);
            }
            else{
                invWithoutPay.add(inv);
            }
        }
        m.writeMasterJustInv(invWithoutPay);
        m.writeMasterErrors();
        m.writeMasterStat();
        m.closeWriter();
    }
}