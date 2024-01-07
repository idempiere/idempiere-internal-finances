/**********************************************************************
* This file is part of iDempiere ERP Open Source                      *
* http://www.idempiere.org                                            *
*                                                                     *
* Copyright (C) Contributors                                          *
*                                                                     *
* This program is free software; you can redistribute it and/or       *
* modify it under the terms of the GNU General Public License         *
* as published by the Free Software Foundation; either version 2      *
* of the License, or (at your option) any later version.              *
*                                                                     *
* This program is distributed in the hope that it will be useful,     *
* but WITHOUT ANY WARRANTY; without even the implied warranty of      *
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the        *
* GNU General Public License for more details.                        *
*                                                                     *
* You should have received a copy of the GNU General Public License   *
* along with this program; if not, write to the Free Software         *
* Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,          *
* MA 02110-1301, USA.                                                 *
*                                                                     *
* Contributors:                                                       *
* - Carlos Ruiz - globalqss                                           *
**********************************************************************/

package org.idempiere.internal.finances.model;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import org.compiere.model.MPayment;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Util;

public class PaymentUtils {

	/**
	 * Validate if a paypal transaction already exists in database
	 * @param payment
	 * @return message if duplicated, null if OK
	 */
	public static String isDupPaypalTrx(MPayment payment) {
		if (   payment.getAD_Client_ID() == Constants.AD_Client_ID_IDEMPIERE
			&& payment.getC_BankAccount_ID() == Constants.C_BankAccount_ID_PAYPAL
			&& payment.getC_BPartner_ID() != Constants.C_BPartner_ID_PAYPAL
			&& (   payment.is_ValueChanged(MPayment.COLUMNNAME_Orig_TrxID)
				|| payment.is_ValueChanged(MPayment.COLUMNNAME_C_BankAccount_ID)
				|| payment.is_ValueChanged(MPayment.COLUMNNAME_C_BPartner_ID))) {

			if (Util.isEmpty(payment.getOrig_TrxID()))
				return "Fill Mandatory Original Transaction ID";

			final String sql = "SELECT COUNT(*) FROM C_Payment WHERE AD_Client_ID=? AND C_BankAccount_ID=? AND Orig_TrxID=? AND C_Payment_ID!=? AND C_BPartner_ID!=?";
			int cnt = DB.getSQLValue(payment.get_TrxName(), sql, payment.getAD_Client_ID(), payment.getC_BankAccount_ID(), payment.getOrig_TrxID(), payment.getC_Payment_ID(), Constants.C_BPartner_ID_PAYPAL);
			if (cnt > 0)
				return "Original Transaction ID already exists";

		}
		return null;
	}

	/**
	 * Create a payment for the paypal fee (filled in the TaxAmt)
	 * @param po
	 * @return
	 */
	public static String createPaypalFeePayment(MPayment payment) {

		if (payment.getC_BPartner_ID() == Constants.C_BPartner_ID_PAYPAL)
			return null;

		if (payment.getC_BankAccount_ID() != Constants.C_BankAccount_ID_PAYPAL)
			return null;

		if (payment.getTaxAmt().signum() == 0)
			return null;

		MPayment paymentFee = new MPayment(payment.getCtx(), 0, payment.get_TrxName());
		paymentFee.setAD_Org_ID(payment.getAD_Org_ID());
		paymentFee.setC_BankAccount_ID(payment.getC_BankAccount_ID());
		paymentFee.setC_DocType_ID(Constants.C_DocType_ID_PAYPAL_FEE);
		paymentFee.setIsReceipt(false);
		paymentFee.setDocumentNo(payment.getDocumentNo());
		paymentFee.setDateTrx(payment.getDateTrx());
		paymentFee.setDateAcct(payment.getDateAcct());
		paymentFee.setC_BPartner_ID(Constants.C_BPartner_ID_PAYPAL);
		paymentFee.setC_Charge_ID(Constants.C_Charge_ID_PAYPAL_FEE);
		paymentFee.setPayAmt(payment.getTaxAmt()); // Paypal Fee
		paymentFee.setC_Currency_ID(payment.getC_Currency_ID());
		paymentFee.setC_ConversionType_ID(payment.getC_ConversionType_ID());
		paymentFee.setIsOverrideCurrencyRate(payment.isOverrideCurrencyRate());
		paymentFee.setTenderType(MPayment.TENDERTYPE_DirectDeposit);
		paymentFee.setTaxAmt(Env.ZERO);
		paymentFee.setOrig_TrxID(payment.getOrig_TrxID());

		if (paymentFee.isOverrideCurrencyRate()) {
			paymentFee.setCurrencyRate(payment.getCurrencyRate());
			BigDecimal convertedAmt = paymentFee.getPayAmt().multiply(paymentFee.getCurrencyRate(), MathContext.UNLIMITED);
			paymentFee.setConvertedAmt(convertedAmt);
		}

		BigDecimal percent = payment.getTaxAmt().multiply(Env.ONEHUNDRED).divide(payment.getPayAmt(), RoundingMode.HALF_UP).setScale(2, RoundingMode.HALF_UP);
		StringBuilder description = new StringBuilder("Transaction fee ")
				.append(payment.getDocumentNo())
				.append(" of ")
				.append(percent)
				.append("% from ")
				.append(payment.getCurrencyISO())
				.append(" ")
				.append(DisplayType.getNumberFormat(DisplayType.CostPrice).format(payment.getPayAmt()));
		paymentFee.setDescription(description.toString());

		paymentFee.saveEx();

		if (!paymentFee.processIt(MPayment.DOCACTION_Complete))
			return "Failing completing paypal fee payment -> " + paymentFee.getProcessMsg();
		paymentFee.saveEx();

		return null;
	}
	
}
