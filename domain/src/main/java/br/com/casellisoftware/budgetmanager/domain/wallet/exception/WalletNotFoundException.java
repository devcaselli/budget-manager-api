package br.com.casellisoftware.budgetmanager.domain.wallet.exception;

public class WalletNotFoundException extends  RuntimeException{

    public WalletNotFoundException(){
        super();
    }

    public  WalletNotFoundException(String message){
        super(message);
    }
}
