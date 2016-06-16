package com.handysparksoft.flyingbirds;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;

/**
 * Created by davasens on 10/20/2015.
 */
public class UserAccountInfo {

    private Context context;

    public UserAccountInfo(Context context) {
        this.context = context;
    }

    public String getUserAccount() {
        AccountManager manager = (AccountManager) context.getSystemService(context.ACCOUNT_SERVICE);
        Account[] list = manager.getAccounts();
        String gmail = null;

        for(Account account: list){
            if(account.type.equalsIgnoreCase("com.google")) {
                gmail = account.name;
                break;
            }
        }
        return gmail;
    }

    public String getFormattedUserAccount() {
        String result = getUserAccount();
        result = result.split("@")[0];
        result = result.replaceAll("\\.", "_").replaceAll("\\#","_").replaceAll("\\$", "_").replaceAll("\\[", "(").replaceAll("\\]", ")");
        return result;
    }

    private String getUserName() {
        String result = "";
        Cursor c = context.getContentResolver().query(ContactsContract.Profile.CONTENT_URI, null, null, null, null);
        int count = c.getCount();
        String[] columnNames = c.getColumnNames();
        boolean b = c.moveToFirst();
        int position = c.getPosition();
        if (count == 1 && position == 0) {
            for (int j = 0; j < columnNames.length; j++) {
                String columnName = columnNames[j];
                if (columnName.equals(ContactsContract.Contacts.DISPLAY_NAME)) {
                    String columnValue = c.getString(c.getColumnIndex(columnName));
                    result = columnValue;
                }
            }
        }
        c.close();
        return result;
    }

    //Si no encuentra el UserName usa la UserAccount
    public String getUserNameOrFormattedAccount() {
        String result = getUserName();
        if (result == null || result == "" || result.length() == 0) {
            result = getFormattedUserAccount();
        }
        return result;
    }
}
