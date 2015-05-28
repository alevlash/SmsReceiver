package com.smsreceiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SmsReceiver extends BroadcastReceiver {

	// Get the oIbject of SmsManager
	final SmsManager sms = SmsManager.getDefault();
	final Compressor compressor = new Compressor();

	public void onReceive(Context context, Intent intent) {

		// Retrieves a map of extended data from the intent.
		final Bundle bundle = intent.getExtras();

		try {

			if (bundle != null) {

				final Object[] pdusObj = (Object[]) bundle.get("pdus");

				for (int i = 0; i < pdusObj.length; i++) {

					SmsMessage currentMessage = SmsMessage.createFromPdu((byte[]) pdusObj[i]);
					String phoneNumber = currentMessage.getDisplayOriginatingAddress();

			        String senderNum = phoneNumber;
			        String message = currentMessage.getDisplayMessageBody();

			        Log.i("SmsReceiver", "senderNum: "+ senderNum + "; message: " + message);
					
					String compressedMessage = "";
					if (message.toLowerCase().startsWith("zipit:")) {
		                compressedMessage = compressor.compress(message.substring(6));
						sms.sendTextMessage(phoneNumber, null, compressedMessage, null, null);
					}
					
			        int duration = Toast.LENGTH_LONG;
					Toast toast = Toast.makeText(context, "senderNum: "+ senderNum + ", message: " + message + ", zipped: " + compressedMessage, duration);
			    	toast.show();			

				} // end for loop
			} // bundle is null

		} catch (Exception e) {
			Log.e("SmsReceiver", "Exception smsReceiver" +e);

		}
	}


	private static class Compressor {

		private static final Map<Integer, Character> INDEX_TO_LETTER_MAP;
		private static final Character TILDE = '~';
		private static final int MIN_LENGTH_TO_COMPRESS = 4;

		static {
			INDEX_TO_LETTER_MAP = new HashMap<Integer, Character>();
			int position = 0;
			for (Character c='A'; c<='Z'; c++) {
				INDEX_TO_LETTER_MAP.put(++position, c);
			}
		}

		public String compress(String input) {
			StringBuffer output = new StringBuffer();
			List<Character> buffer = new ArrayList<Character>();

			for (Character c : input.toCharArray()) {
				if (buffer.isEmpty()) {
					buffer.add(c);
				} else if (buffer.size() < INDEX_TO_LETTER_MAP.size() && c.equals(buffer.get(0))) {
					buffer.add(c);
				} else {
					output.append(getOutputFromBuffer(buffer));
					buffer.clear();
					buffer.add(c);
				}
			}

			output.append(getOutputFromBuffer(buffer));

			return output.toString();
		}

		private String getOutputFromBuffer(List<Character> buffer) {
			StringBuffer output = new StringBuffer(); 

			if (buffer.size() >= MIN_LENGTH_TO_COMPRESS || buffer.get(0).equals(TILDE)) {
				output.append(TILDE);
				output.append(INDEX_TO_LETTER_MAP.get(buffer.size()));
				output.append(buffer.get(0));
			} else {
				for (Character c: buffer) {
					output.append(c);
				}
			}

			return output.toString();
		}

	}

}
