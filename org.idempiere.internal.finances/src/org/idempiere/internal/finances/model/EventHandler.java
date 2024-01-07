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

import org.adempiere.base.event.AbstractEventHandler;
import org.adempiere.base.event.IEventTopics;
import org.compiere.model.MPayment;
import org.compiere.model.PO;
import org.compiere.util.CLogger;
import org.osgi.service.event.Event;


/**
 *	Event Handler for internal finances of iDempiere
 */
public class EventHandler extends AbstractEventHandler
{
	/**	Logger			*/
	private static CLogger log = CLogger.getCLogger(EventHandler.class);

	@Override
	protected void initialize() {
		log.warning("");

		//	Tables to be monitored
		registerTableEvent(IEventTopics.PO_AFTER_NEW, MPayment.Table_Name);
		registerTableEvent(IEventTopics.PO_AFTER_CHANGE, MPayment.Table_Name);
		registerTableEvent(IEventTopics.DOC_AFTER_COMPLETE, MPayment.Table_Name);
	}

	@Override
	protected void doHandleEvent(Event event) {
		String type = event.getTopic();

		PO po = getPO(event);
		log.info(po.get_TableName() + " Type: "+type);
		String msg;

		// Model Events
		if (po instanceof MPayment && (type.equals(IEventTopics.PO_AFTER_NEW) || type.equals(IEventTopics.PO_AFTER_CHANGE))) {
			msg = PaymentUtils.isDupPaypalTrx((MPayment) po);
			if (msg != null)
				throw new RuntimeException(msg);
		}

		// Doc Events
		if (po instanceof MPayment && type.equals(IEventTopics.DOC_AFTER_COMPLETE)) {
			msg = PaymentUtils.createPaypalFeePayment((MPayment) po);
			if (msg != null)
				throw new RuntimeException(msg);
		}
	}

}	//	EventHandler
