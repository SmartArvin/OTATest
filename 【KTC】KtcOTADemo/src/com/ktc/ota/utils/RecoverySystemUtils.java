package com.ktc.ota.utils;

import android.content.Context;
import android.os.Build;
import android.os.PowerManager;
import android.os.RecoverySystem;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.text.TextUtils;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.Character.UnicodeBlock;
import java.util.Locale;

public class RecoverySystemUtils {
    private static final String TAG = "RecoverySystemUtils";

    /** Used to communicate with recovery.  See bootable/recovery/recovery.c. */
    private static File RECOVERY_DIR = new File("/cache/recovery");
    private static File UPDATE_FLAG_FILE = new File(RECOVERY_DIR, "last_install");
    private static File COMMAND_FILE = new File(RECOVERY_DIR, "command");
    private static File UNCRYPT_FILE = new File(RECOVERY_DIR, "uncrypt_file");
    private static File LOG_FILE = new File(RECOVERY_DIR, "log");

    private void RecoverySystemUtils() { }  // Do not instantiate
    
    public static void installPackage(Context context, File packageFile)
            throws IOException {
        	String filename = packageFile.getCanonicalPath();
        	LogUtil.i(TAG, "filename:  "+filename);
    		
        	if(UNCRYPT_FILE != null && UNCRYPT_FILE.exists()){
        		FileWriter uncryptFile = new FileWriter(UNCRYPT_FILE);
                try {
                    uncryptFile.write(filename + "\n");
                }catch(Exception e){
                	e.printStackTrace();
                } finally {
                	try {
                		if(uncryptFile != null){
                    		uncryptFile.close();
                    	}
					} catch (Exception e2) {
						e2.printStackTrace();
					}
                }
        	}
            LogUtil.w(TAG, "!!! REBOOTING TO INSTALL " + filename + " !!!");

            final String filenameArg = "--update_package=" + filename;
            final String localeArg = "--locale=" + Locale.getDefault().toString();
            bootCommand(context, packageFile , filenameArg, localeArg);
        }

	
    /**
     * Reboot into the recovery system with the supplied argument.
     * @param arg to pass to the recovery utility.
     * @throws IOException if something goes wrong.
     */
    /**
     * Reboot into the recovery system with the supplied argument.
     * @param args to pass to the recovery utility.
     * @throws IOException if something goes wrong.
     */
    private static void bootCommand(Context context, File packageFile , String... args) throws IOException {
        RECOVERY_DIR.mkdirs();  // In case we need it
        COMMAND_FILE.delete();  // In case it's not writable
        LOG_FILE.delete();

        FileWriter command = new FileWriter(COMMAND_FILE);
        try {
            for (String arg : args) {
                if (!TextUtils.isEmpty(arg)) {
                    // MStar Android Patch Begin
                    String cmd = arg;
                    String label = null;
                    String uuid = null;

                    if (cmd.startsWith("--update_package")) {
                        cmd = arg.substring(17, arg.length() -1);
                        if (cmd.startsWith("/cache")) {//Cache
                            command.write("--uuid=mstar-cache");
                            command.write("\n");
                            command.write("--label=mstar-cache");
                            command.write("\n");
                        }else if (cmd.startsWith("/data")){//Data
                        } else {//USB
                        	if(Build.VERSION.SDK_INT < 23){
                        		RecoverySystem.installPackage(context, packageFile);
                        		return ;
                        	}else{
                        		StorageManager mStorageManager = (StorageManager)context.getSystemService(Context.STORAGE_SERVICE);
                                StorageVolume vol = mStorageManager.getStorageVolume(new File(cmd));
                                uuid = "--uuid=" + vol.getUuid();
                                label = "--label=" + utf8ToUnicode(vol.getFsLabel());
                                command.write(uuid);
                                command.write("\n");
                                command.write(label);
                                command.write("\n");
                        	}
                        }
                    }
                    // MStar Android Patch End
                    command.write(arg);
                    command.write("\n");
                }
            }
        } finally {
            command.close();
        }

        // Having written the command file, go ahead and reboot
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        pm.reboot(PowerManager.REBOOT_RECOVERY);

        throw new IOException("Reboot failed (no permissions?)");
    }

 // MStar Android Patch Begin
    private static String utf8ToUnicode(String inStr) {
        char[] myBuffer = inStr.toCharArray();
        StringBuffer sb = new StringBuffer();

        for (int i = 0;i < inStr.length(); i++) {
            UnicodeBlock ub = UnicodeBlock.of(myBuffer[i]);
            if (ub == UnicodeBlock.BASIC_LATIN) {
                String unicode = "\\u" + myBuffer[i];
                sb.append(unicode);
            } else {
                short s = (short)myBuffer[i];
                String hexS = Integer.toHexString(s);
                String unicode = "\\u" + hexS;
                sb.append(unicode.toLowerCase());
            }
        }
        return sb.toString();
    }
    // MStar Android Patch End
}
