/*
 * $Header: //depot/FXCM/New_CurrentSystem/Main/FXCM_SRC/TRADING_SDK/fxcm-api/src/main/QATest.java#19 $
 *
 * Copyright (c) 2011 FXCM, LLC. All Rights Reserved.
 * 32 Old Slip, 10th Floor, New York, NY 10005 USA
 *
 * THIS SOFTWARE IS THE CONFIDENTIAL AND PROPRIETARY INFORMATION OF
 * FXCM, LLC. ("CONFIDENTIAL INFORMATION"). YOU SHALL NOT DISCLOSE
 * SUCH CONFIDENTIAL INFORMATION AND SHALL USE IT ONLY IN ACCORDANCE
 * WITH THE TERMS OF THE LICENSE AGREEMENT YOU ENTERED INTO WITH
 * FXCM.
 *
 * $History: $
 * 06/03/2005   Andre Mermegas  sendMessage(), follow up to interface changes
 * 06/13/2005   Miron rewritten to followup coding rules follow up to FXCMLoginProperties changes
 * 07/23/2005   Miron MessageAnalyzer removed, follow up changes
 * 08/01/2005   Miron only !isDeleted() order is to delete or modify fill all requred fields for OrderReplace/Cancel
 * 08/12/2005   Miron all inital request are based on login id
 * 08/18/2005   Andre Mermegas: update
 * 10/18/2005   Andre Mermegas: update for 1.3 backward compat
 * 11/10/2005   Andre Mermegas: added test for Historical Snapshot Closed Position Reports, Order Status Request
 * 11/14/2005   Andre Mermegas: updated order status request test for clarity
 * 11/18/2005   Andre Mermegas: added test for true market orders
 * 12/02/2005   Andre Mermegas: updated test for true market order, minor cleanup
 * 01/03/2006   Andre Mermegas: added processing of SecurityStatus
 * 06/02/2006   Andre Mermegas: refactoring, added testCloseTrueMarketOrder
 * 09/12/2006   Andre Mermeags: added open/close range order tests
 * 02/12/2009   Andre Mermegas: add orderhistory example
 * 02/26/2009   Andre Mermegas: add OTO example
 * 07/29/2010   Andre Mermegas: add COCO,DAY,LSTE,ELS
 * 08/10/2010   Andre Mermegas: touchup
 * 09/01/2010   Andre Mermegas: add qty change test to update entry order
 * 08/02/2011   Andre Mermegas: touchup testHistoricalSnapshot
 */

import com.fxcm.external.api.transport.FXCMLoginProperties;
import com.fxcm.external.api.transport.GatewayFactory;
import com.fxcm.external.api.transport.IGateway;
import com.fxcm.external.api.transport.listeners.IGenericMessageListener;
import com.fxcm.external.api.transport.listeners.IStatusMessageListener;
import com.fxcm.external.api.util.MessageAnalyzer;
import com.fxcm.external.api.util.MessageGenerator;
import com.fxcm.external.api.util.OrdStatusRequestType;
import com.fxcm.fix.ContingencyTypeFactory;
import com.fxcm.fix.FXCMOrdStatusFactory;
import com.fxcm.fix.FXCMTimingIntervalFactory;
import com.fxcm.fix.IFixDefs;
import com.fxcm.fix.ISide;
import com.fxcm.fix.Instrument;
import com.fxcm.fix.NotDefinedException;
import com.fxcm.fix.OrdStatusFactory;
import com.fxcm.fix.OrdTypeFactory;
import com.fxcm.fix.PegInstruction;
import com.fxcm.fix.SideFactory;
import com.fxcm.fix.SubscriptionRequestTypeFactory;
import com.fxcm.fix.TimeInForceFactory;
import com.fxcm.fix.TradingSecurity;
import com.fxcm.fix.UTCDate;
import com.fxcm.fix.UTCTimeOnly;
import com.fxcm.fix.UTCTimestamp;
import com.fxcm.fix.admin.Logout;
import com.fxcm.fix.other.BusinessMessageReject;
import com.fxcm.fix.other.UserRequest;
import com.fxcm.fix.other.UserResponse;
import com.fxcm.fix.posttrade.ClosedPositionReport;
import com.fxcm.fix.posttrade.CollateralInquiryAck;
import com.fxcm.fix.posttrade.CollateralReport;
import com.fxcm.fix.posttrade.PositionReport;
import com.fxcm.fix.posttrade.RequestForPositionsAck;
import com.fxcm.fix.pretrade.MarketDataRequest;
import com.fxcm.fix.pretrade.MarketDataRequestReject;
import com.fxcm.fix.pretrade.MarketDataSnapshot;
import com.fxcm.fix.pretrade.Quote;
import com.fxcm.fix.pretrade.QuoteRequest;
import com.fxcm.fix.pretrade.QuoteResponse;
import com.fxcm.fix.pretrade.SecurityList;
import com.fxcm.fix.pretrade.SecurityListRequest;
import com.fxcm.fix.pretrade.SecurityStatus;
import com.fxcm.fix.pretrade.SecurityStatusRequest;
import com.fxcm.fix.pretrade.TradingSessionStatus;
import com.fxcm.fix.trade.ExecutionReport;
import com.fxcm.fix.trade.OrderCancelReplaceRequest;
import com.fxcm.fix.trade.OrderCancelRequest;
import com.fxcm.fix.trade.OrderList;
import com.fxcm.fix.trade.OrderMassStatusRequest;
import com.fxcm.fix.trade.OrderSingle;
import com.fxcm.messaging.ISessionStatus;
import com.fxcm.messaging.ITransportable;
import com.fxcm.messaging.util.fix.FXCMCommandType;
import com.fxcm.util.Util;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 */
public class QATest {
    private String mAccountMassID;
    private String mConfigFile;
    private IGateway mFxcmGateway;
    private String mOpenOrderMassID;
    private String mOpenPositionMassID;
    private String mPassword;
    private String mServer;
    private String mStation;
    private IStatusMessageListener mStatusListener;
    private String mUsername;

    public QATest(String aUsername, String aPassword, String aStation, String aServer, String aConfigFile) {
        mServer = aServer;
        mUsername = aUsername;
        mPassword = aPassword;
        mStation = aStation;
        mConfigFile = aConfigFile;
    }

    private boolean doResult(final MessageTestHandler aMessageTestHandler) {
        new Thread(
                new Runnable() {
                    public void run() {
                        setup(aMessageTestHandler, false);
                    }
                }).start();
        int expiration = 60; //seconds
        while (!aMessageTestHandler.isSuccess() && expiration > 0) {
            try {
                Thread.sleep(1000);
                expiration--;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (aMessageTestHandler.isSuccess()) {
            System.out.println("done waiting.\nstatus = " + aMessageTestHandler.isSuccess());
        } else {
            System.err.println("done waiting.\nstatus = " + aMessageTestHandler.isSuccess());
        }
        mFxcmGateway.removeGenericMessageListener(aMessageTestHandler);
        mFxcmGateway.removeStatusMessageListener(mStatusListener);
        mFxcmGateway.logout();
        return aMessageTestHandler.isSuccess();
    }

    private void handleMessage(ITransportable aMessage, List aAccounts, MessageTestHandler aMessageTestHandler) {
        if (aMessage instanceof CollateralReport) {
            CollateralReport cr = (CollateralReport) aMessage;
            if (safeEquals(mAccountMassID, cr.getRequestID()) && aAccounts != null) {
                aAccounts.add(cr);
            }
            aMessageTestHandler.process(cr);
        } else if (aMessage instanceof CollateralInquiryAck) {
            aMessageTestHandler.process((CollateralInquiryAck) aMessage);
        } else if (aMessage instanceof ExecutionReport) {
            aMessageTestHandler.process((ExecutionReport) aMessage);
        } else if (aMessage instanceof ClosedPositionReport) {
            aMessageTestHandler.process((ClosedPositionReport) aMessage);
        } else if (aMessage instanceof PositionReport) {
            aMessageTestHandler.process((PositionReport) aMessage);
        } else if (aMessage instanceof BusinessMessageReject) {
            aMessageTestHandler.process((BusinessMessageReject) aMessage);
        } else if (aMessage instanceof RequestForPositionsAck) {
            aMessageTestHandler.process((RequestForPositionsAck) aMessage);
        } else if (aMessage instanceof TradingSessionStatus) {
            aMessageTestHandler.process((TradingSessionStatus) aMessage);
        } else if (aMessage instanceof SecurityStatus) {
            aMessageTestHandler.process((SecurityStatus) aMessage);
        } else if (aMessage instanceof Quote) {
            aMessageTestHandler.process((Quote) aMessage);
        } else if (aMessage instanceof MarketDataSnapshot) {
            aMessageTestHandler.process((MarketDataSnapshot) aMessage);
        } else if (aMessage instanceof SecurityList) {
            aMessageTestHandler.process((SecurityList) aMessage);
        } else if (aMessage instanceof UserResponse) {
            aMessageTestHandler.process((UserResponse) aMessage);
        } else if (aMessage instanceof Logout) {
            aMessageTestHandler.process((Logout) aMessage);
        } else if (aMessage instanceof MarketDataRequestReject) {
            aMessageTestHandler.process((MarketDataRequestReject) aMessage);
        }
    }

    private static void runTest(String[] aArgs) {
        String aCommand = aArgs[4];
        String arg5 = null;
        if (aArgs.length > 5) {
            arg5 = aArgs[5];
        }

        System.out.print("Argument = " + aCommand + ", ");
        QATest qaTest = new QATest(aArgs[0], aArgs[1], aArgs[2], aArgs[3], arg5);
        if ("CPQO".equalsIgnoreCase(aCommand)) {
            qaTest.testCreatePreviouslyQuotedOrder();
        } else if ("SSLMO".equalsIgnoreCase(aCommand)) {
            qaTest.testSetSLMarketOrder();
        } else if ("USLMO".equalsIgnoreCase(aCommand)) {
            qaTest.testUpdateSLMarketOrder();
        } else if ("DSLMO".equalsIgnoreCase(aCommand)) {
            qaTest.testDeleteSLMarketOrder();
        } else if ("CEO".equalsIgnoreCase(aCommand)) {
            qaTest.testCreateEntryOrder(false);
        } else if ("SSLEO".equalsIgnoreCase(aCommand)) {
            qaTest.testSetSLEntryOrder();
        } else if ("USLEO".equalsIgnoreCase(aCommand)) {
            qaTest.testUpdateSLEntryOrder();
        } else if ("DSLEO".equalsIgnoreCase(aCommand)) {
            qaTest.testDeleteSLEntryOrder();
        } else if ("DEO".equalsIgnoreCase(aCommand)) {
            qaTest.testDeleteEntryOrder();
        } else if ("CLOSEPQO".equalsIgnoreCase(aCommand)) {
            qaTest.testClosePreviouslyQuotedOrder();
        } else if ("URQEO".equalsIgnoreCase(aCommand)) {
            qaTest.testUpdateRateQtyEntryOrder();
        } else if ("LISTEN".equalsIgnoreCase(aCommand)) {
            qaTest.testListen();
        } else if ("PURGE".equalsIgnoreCase(aCommand)) {
            qaTest.testPurgeAll();
        } else if ("RECONNECT".equalsIgnoreCase(aCommand)) {
            qaTest.testReconnect();
        } else if ("OCM".equalsIgnoreCase(aCommand)) {
            qaTest.testOpenCloseMarket();
        } else if ("IRC".equalsIgnoreCase(aCommand)) {
            qaTest.testInterestRateChange();
        } else if ("ABC".equalsIgnoreCase(aCommand)) {
            qaTest.testAccountBalanceChange();
        } else if ("RMO".equalsIgnoreCase(aCommand)) {
            qaTest.testRejectMarketOrder();
        } else if ("ADIR".equalsIgnoreCase(aCommand)) {
            qaTest.testAcceptDealerInterventionRequote();
        } else if ("RDIR".equalsIgnoreCase(aCommand)) {
            qaTest.testRejectDealerInterventionRequote();
        } else if ("OSR".equalsIgnoreCase(aCommand)) {
            qaTest.testOrderStatusRequest();
        } else if ("HS".equalsIgnoreCase(aCommand)) {
            qaTest.testHistoricalSnapshot();
        } else if ("CTMO".equalsIgnoreCase(aCommand)) {
            qaTest.testCreateTrueMarketOrder();
        } else if ("RFQ".equalsIgnoreCase(aCommand)) {
            qaTest.testRFQ();
        } else if ("SSR".equalsIgnoreCase(aCommand)) {
            qaTest.testSecurityStatusRequest();
        } else if ("SLR".equalsIgnoreCase(aCommand)) {
            qaTest.testSecurityListRequest();
        } else if ("CLOSETMO".equalsIgnoreCase(aCommand)) {
            qaTest.testCloseTrueMarketOrder();
        } else if ("ALL".equalsIgnoreCase(aCommand)) {
            qaTest.testAll();
        } else if ("MDR".equalsIgnoreCase(aCommand)) {
            qaTest.testMarketDataRequest();
        } else if ("MDH".equalsIgnoreCase(aCommand)) {
            qaTest.testMarketDataHistory();
        } else if ("OPENRANGE".equalsIgnoreCase(aCommand)) {
            qaTest.testOpenRangeOrder();
        } else if ("CLOSERANGE".equalsIgnoreCase(aCommand)) {
            qaTest.testCloseRangeOrder();
        } else if ("OL".equalsIgnoreCase(aCommand)) {
            qaTest.testOrderList();
        } else if ("OTOCO".equalsIgnoreCase(aCommand)) {
            qaTest.testOTOCO();
        } else if ("OCO".equalsIgnoreCase(aCommand)) {
            qaTest.testOCO();
        } else if ("COCO".equalsIgnoreCase(aCommand)) {
            qaTest.testComplexOCO();
        } else if ("OPENLIMIT".equalsIgnoreCase(aCommand)) {
            qaTest.testOpenLimit();
        } else if ("CLOSELIMIT".equalsIgnoreCase(aCommand)) {
            qaTest.testCloseLimit();
        } else if ("USERSTATUS".equalsIgnoreCase(aCommand)) {
            qaTest.testUserStatus();
        } else if ("OH".equalsIgnoreCase(aCommand)) {
            qaTest.testOrderHistory();
        } else if ("OTO".equalsIgnoreCase(aCommand)) {
            qaTest.testOTO();
        } else if ("NQC".equalsIgnoreCase(aCommand)) {
            qaTest.testNetQuantityClose();
        } else if ("NQSL".equalsIgnoreCase(aCommand)) {
            qaTest.testNetQuantityStopLimit();
        } else if ("ATMARKET".equalsIgnoreCase(aCommand)) {
            qaTest.testAtMarketPointsOrder();
        } else if ("LSTE".equalsIgnoreCase(aCommand)) {
            qaTest.testCreateTrailingEntryOrder();
        } else if ("SSLTEO".equalsIgnoreCase(aCommand)) {
            qaTest.testSetSLTEntryOrder();
        } else if ("ELS".equalsIgnoreCase(aCommand)) {
            qaTest.testELS();
        } else if ("ELSPEG".equalsIgnoreCase(aCommand)) {
            qaTest.testELSPeg();
        } else if ("DAY".equalsIgnoreCase(aCommand)) {
            qaTest.testCreateEntryOrder(true);
        } else if ("EQUITY".equalsIgnoreCase(aCommand)) {
            qaTest.testRequestEquity();
        } else {
            System.out.println("Unknown Command: " + aCommand);
        }
    }

    public static boolean safeEquals(String aString1, String aString2) {
        if (aString1 == null || aString2 == null) {
            return false;
        } else {
            return aString1.equals(aString2);
        }
    }

    private void setup(IGenericMessageListener aGenericListener, boolean aPrintStatus) {
        if (mFxcmGateway == null) {
            // step 1: get an instance of IGateway from the GatewayFactory
            mFxcmGateway = GatewayFactory.createGateway();
        }
        /*
            step 2: register a generic message listener with the gateway, this
            listener in particular gets all messages that are related to the trading
            platform Quote,OrderSingle,ExecutionReport, etc...
        */
        mFxcmGateway.registerGenericMessageListener(aGenericListener);
        mStatusListener = new DefaultStatusListener(aPrintStatus);
        mFxcmGateway.registerStatusMessageListener(mStatusListener);
        try {
            /*
                step 3: call login on the gateway, this method takes an instance of FXCMLoginProperties
                which takes 4 parameters: username,password,terminal and server or path to a Hosts.xml
                file which it uses for resolving servers. As soon as the login  method executes your listeners begin
                receiving asynch messages from the FXCM servers.
            */
            if (!mFxcmGateway.isConnected()) {
                System.out.println("client: login");
                FXCMLoginProperties properties = new FXCMLoginProperties(mUsername, mPassword, mStation, mServer, mConfigFile);
                /*
                Properties p = new Properties();
                p.put(IUserSession.PIN, "121212");
                properties.setProperties(p);
                */
                mFxcmGateway.login(properties);
            }
            //after login you must retrieve your trading session status and get accounts to receive messages
            System.out.println("client: requestTradingSessionStatus");
            mFxcmGateway.requestTradingSessionStatus();
            mAccountMassID = mFxcmGateway.requestAccounts();
            mOpenOrderMassID = mFxcmGateway.requestOpenOrders();
            mOpenPositionMassID = mFxcmGateway.requestOpenPositions();
            System.out.println("client: all done");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Order Flow Use Case 8
     */
    public boolean testAcceptDealerInterventionRequote() {
        final String cem = Util.getCurrentlyExecutingMethod();
        System.out.println(cem);
        class GenericListener extends MessageTestHandler {
            private boolean mOrder = true;
            private String mRequoteID;

            public void process(CollateralReport aCollateralReport) {
                try {
                    if (mOrder) {
                        mOrder = false;
                        OrderSingle orderSingle = MessageGenerator.generateMarketOrder(
                                aCollateralReport.getAccount(),
                                aCollateralReport.getQuantity(),
                                SideFactory.BUY,
                                "EUR/USD",
                                cem);
                        String requestId = mFxcmGateway.sendMessage(orderSingle);
                        System.out.println("client: order requestid = " + requestId);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            public void process(PositionReport aPositionReport) {
                if (aPositionReport.getClOrdID().equals(mRequoteID)) {
                    System.out.println("aPositionReport = " + aPositionReport);
                    mFxcmGateway.logout();
                    System.out.println("completed successfully");
                }
            }

            public void process(Quote aQuote) {
                System.out.println("aQuote = " + aQuote);
                if (aQuote.getQuoteID().startsWith(FXCMCommandType.REQUOTE_PREFIX)) {
                    try {
                        OrderSingle orderSingle =
                                MessageGenerator.generateAcceptOrder(aQuote.getQuoteID(), "accept requote");
                        mRequoteID = mFxcmGateway.sendMessage(orderSingle);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            public void messageArrived(ITransportable aMessage) {
                handleMessage(aMessage, null, this);
            }
        }

        return doResult(new GenericListener());
    }

    /**
     * Order Flow Use Case 3
     */
    public boolean testAccountBalanceChange() {
        System.out.println(Util.getCurrentlyExecutingMethod());
        class GenericListener extends MessageTestHandler {
            public void process(CollateralReport aCollateralReport) {
                System.out.println(
                        "client: collateral report cash outstanding = " + aCollateralReport.getCashOutstanding());
            }

            public void messageArrived(ITransportable aMessage) {
                handleMessage(aMessage, null, this);
            }
        }

        return doResult(new GenericListener());
    }

    public void testAll() {
        Date start = new Date(System.currentTimeMillis());
        Map map = new TreeMap();
        map.put("CLOSEPQO", Boolean.valueOf(testClosePreviouslyQuotedOrder()));
        map.put("CEO", Boolean.valueOf(testCreateEntryOrder(false)));
        map.put("CPQO", Boolean.valueOf(testCreatePreviouslyQuotedOrder()));
        map.put("CTMO", Boolean.valueOf(testCreateTrueMarketOrder()));
        map.put("DEO", Boolean.valueOf(testDeleteEntryOrder()));
        map.put("DSLEO", Boolean.valueOf(testDeleteSLEntryOrder()));
        map.put("DSLMO", Boolean.valueOf(testDeleteSLMarketOrder()));
        map.put("HS", Boolean.valueOf(testHistoricalSnapshot()));
        map.put("OSR", Boolean.valueOf(testOrderStatusRequest()));
        map.put("SLR", Boolean.valueOf(testSecurityListRequest()));
        map.put("SSR", Boolean.valueOf(testSecurityStatusRequest()));
        map.put("SSLEO", Boolean.valueOf(testSetSLEntryOrder()));
        map.put("SSLMO", Boolean.valueOf(testSetSLMarketOrder()));
        map.put("URQEO", Boolean.valueOf(testUpdateRateQtyEntryOrder()));
        map.put("USLEO", Boolean.valueOf(testUpdateSLEntryOrder()));
        map.put("USLMO", Boolean.valueOf(testUpdateSLMarketOrder()));
        map.put("CLOSETMO", Boolean.valueOf(testCloseTrueMarketOrder()));
        map.put("MDR", Boolean.valueOf(testMarketDataRequest()));
        map.put("MDH", Boolean.valueOf(testMarketDataHistory()));
        map.put("USERSTATUS", Boolean.valueOf(testUserStatus()));
        map.put("OL", Boolean.valueOf(testOrderList()));
        map.put("OPENRANGE", Boolean.valueOf(testOpenRangeOrder()));
        map.put("CLOSERANGE", Boolean.valueOf(testCloseRangeOrder()));
        map.put("OPENLIMIT", Boolean.valueOf(testOpenLimit()));
        map.put("CLOSELIMIT", Boolean.valueOf(testCloseLimit()));
        map.put("OH", Boolean.valueOf(testOrderHistory()));
        map.put("OTO", Boolean.valueOf(testOTO()));
        map.put("OCO", Boolean.valueOf(testOCO()));
        map.put("COCO", Boolean.valueOf(testComplexOCO()));
        map.put("NQC", Boolean.valueOf(testNetQuantityClose()));
        map.put("NQSL", Boolean.valueOf(testNetQuantityStopLimit()));
        map.put("ATMARKET", Boolean.valueOf(testAtMarketPointsOrder()));
        map.put("LSTE", Boolean.valueOf(testCreateTrailingEntryOrder()));
        map.put("SSLTEO", Boolean.valueOf(testSetSLTEntryOrder()));
        map.put("ELS", Boolean.valueOf(testELS()));
        map.put("ELSPEG", Boolean.valueOf(testELSPeg()));
        map.put("DAY", Boolean.valueOf(testCreateEntryOrder(true)));

        System.out.println("\nstart = " + start);
        Object[] keys = map.keySet().toArray();
        for (int i = 0; i < keys.length; i++) {
            Object key = keys[i];
            Boolean value = (Boolean) map.get(key);
            if (value.booleanValue()) {
                System.out.print("SUCCESS ");
                System.out.println(key);
                map.remove(key);
            }
        }
        keys = map.keySet().toArray();
        for (int i = 0; i < keys.length; i++) {
            Object key = keys[i];
            Boolean value = (Boolean) map.get(key);
            if (!value.booleanValue()) {
                System.err.print("FAILURE ");
                System.err.println(key);
            }
        }
        System.out.println("end = " + new Date(System.currentTimeMillis()) + "\n");
    }

    public boolean testCloseLimit() {
        final String cem = Util.getCurrentlyExecutingMethod();
        System.out.println(cem);
        class GenericListener extends MessageTestHandler {
            private List mAccounts = new ArrayList();
            private boolean mOrder = true;
            private boolean mFlag = true;
            private String mRequestId;
            private String mLimitReqID;
            private PositionReport mPositionReport;

            public void process(MarketDataSnapshot aMarketDataSnapshot) {
                try {
                    if ("EUR/USD".equals(aMarketDataSnapshot.getInstrument().getSymbol()) && mPositionReport != null) {
                        try {
                            System.out.println("client: trying to set a close limit on the position in 2 seconds");
                            OrderSingle order = MessageGenerator.generateCloseOrder(
                                    aMarketDataSnapshot.getBidClose(),
                                    mPositionReport.getFXCMPosID(),
                                    mPositionReport.getAccount(),
                                    mPositionReport.getPositionQty().getQty(),
                                    SideFactory.SELL,
                                    mPositionReport.getInstrument().getSymbol(),
                                    cem);
                            order.setOrdType(OrdTypeFactory.LIMIT);
                            order.setTimeInForce(TimeInForceFactory.IMMEDIATE_OR_CANCEL);
                            mLimitReqID = mFxcmGateway.sendMessage(order);
                            System.out.println("client: close limit order reqId = " + mLimitReqID);
                            mPositionReport = null;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } catch (NotDefinedException e) {
                    e.printStackTrace();
                }
            }

            public void process(PositionReport aPositionReport) {
                super.process(aPositionReport);
                if (mFlag && safeEquals(mRequestId, aPositionReport.getRequestID())) {
                    mPositionReport = aPositionReport;
                    mFlag = false;
                    MarketDataRequest mdr = new MarketDataRequest();
                    mdr.addRelatedSymbol(mTradingSessionStatus.getSecurity("EUR/USD"));
                    mdr.setSubscriptionRequestType(SubscriptionRequestTypeFactory.SUBSCRIBE);
                    mdr.setMDEntryTypeSet(MarketDataRequest.MDENTRYTYPESET_ALL);
                    try {
                        mFxcmGateway.sendMessage(mdr);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            public void process(ClosedPositionReport aClosedPositionReport) {
                super.process(aClosedPositionReport);
                if (aClosedPositionReport.getFXCMCloseClOrdID().equals(mLimitReqID)) {
                    setSuccess(true);
                }
            }

            public void process(CollateralReport aCollateralReport) {
                try {
                    if (mOrder) {
                        mOrder = false;
                        OrderSingle orderSingle = MessageGenerator.generateMarketOrder(
                                aCollateralReport.getAccount(),
                                aCollateralReport.getQuantity(),
                                SideFactory.BUY,
                                "EUR/USD",
                                cem);
                        mRequestId = mFxcmGateway.sendMessage(orderSingle);
                        System.out.println("client: good order requestid = " + mRequestId);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            public void messageArrived(ITransportable aMessage) {
                handleMessage(aMessage, mAccounts, this);
            }
        }

        return doResult(new GenericListener());
    }

    public boolean testClosePreviouslyQuotedOrder() {
        final String cem = Util.getCurrentlyExecutingMethod();
        System.out.println(cem);
        class GenericListener extends MessageTestHandler {
            private List mAccounts = new ArrayList();
            private boolean mOrder = true;
            private String mRequestId;
            private PositionReport mPositionReport;

            public void process(PositionReport aPositionReport) {
                System.out.println("client: incoming pos report = " + aPositionReport);
                if (safeEquals(mRequestId, aPositionReport.getRequestID())) {
                    mPositionReport = aPositionReport;
                }
            }

            public void process(ClosedPositionReport aClosedPositionReport) {
                super.process(aClosedPositionReport);
                if (aClosedPositionReport.getFXCMCloseClOrdID().equals(mRequestId)) {
                    setSuccess(true);
                }
            }

            public void process(MarketDataSnapshot aMarketDataSnapshot) {
                try {
                    if (mOrder && !mAccounts.isEmpty()) {
                        mOrder = false;
                        CollateralReport acct = (CollateralReport) mAccounts.get(0);
                        OrderSingle orderSingle = MessageGenerator.generateOpenOrder(
                                aMarketDataSnapshot.getQuoteID(),
                                aMarketDataSnapshot.getAskClose(),
                                acct.getAccount(),
                                acct.getQuantity(),
                                SideFactory.BUY,
                                aMarketDataSnapshot.getInstrument().getSymbol(),
                                cem);
                        mRequestId = mFxcmGateway.sendMessage(orderSingle);
                        System.out.println("client: good order requestid = " + mRequestId);
                    } else {
                        String snapSym = aMarketDataSnapshot.getInstrument().getSymbol();
                        if (mPositionReport != null && snapSym.equals(mPositionReport.getInstrument().getSymbol())) {
                            System.out.println("client: closing the open position");
                            // set to the opposite side
                            OrderSingle delOrder = MessageGenerator.generateCloseOrder(
                                    aMarketDataSnapshot.getQuoteID(),
                                    aMarketDataSnapshot.getBidClose(),
                                    mPositionReport.getFXCMPosID(),
                                    mPositionReport.getAccount(),
                                    mPositionReport.getPositionQty().getQty(),
                                    SideFactory.SELL,
                                    mPositionReport.getInstrument().getSymbol(),
                                    cem);
                            delOrder.setTimeInForce(TimeInForceFactory.FILL_OR_KILL);
                            mPositionReport = null;
                            mRequestId = mFxcmGateway.sendMessage(delOrder);
                            System.out.println("client: del reqId = " + mRequestId);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            public void messageArrived(ITransportable aMessage) {
                handleMessage(aMessage, mAccounts, this);
            }
        }

        return doResult(new GenericListener());
    }

    public boolean testCloseRangeOrder() {
        final String cem = Util.getCurrentlyExecutingMethod();
        System.out.println(cem);
        class GenericListener extends MessageTestHandler {
            private List mAccounts = new ArrayList();
            private boolean mOrder = true;
            private boolean mFlag = true;
            private String mRequestId;

            public void process(PositionReport aPositionReport) {
                System.out.println("client: incoming pos report = " + aPositionReport);
                if (safeEquals(mRequestId, aPositionReport.getRequestID())) {
                    if (mFlag) {
                        try {
                            ISide side;
                            // flip the side and set the correct price
                            if (aPositionReport.getPositionQty().getSide() == SideFactory.BUY) {
                                side = SideFactory.SELL;
                            } else {
                                side = SideFactory.BUY;
                            }
                            mFlag = false;
                            System.out.println("client: closing the open position in 2 seconds.");
                            // set to the opposite side
                            OrderSingle delOrder = MessageGenerator.generateCloseOrder(
                                    0,
                                    aPositionReport.getFXCMPosID(),
                                    aPositionReport.getAccount(),
                                    aPositionReport.getPositionQty().getQty(),
                                    side,
                                    aPositionReport.getInstrument().getSymbol(),
                                    cem);
                            delOrder.setOrdType(OrdTypeFactory.STOP_LIMIT);
                            double points = .0010;
                            if (side == SideFactory.BUY) {
                                delOrder.setPrice(aPositionReport.getSettlPrice() - points);
                                delOrder.setStopPx(aPositionReport.getSettlPrice() + points);
                            } else {
                                delOrder.setPrice(aPositionReport.getSettlPrice() + points);
                                delOrder.setStopPx(aPositionReport.getSettlPrice() - points);
                            }
                            mRequestId = mFxcmGateway.sendMessage(delOrder);
                            System.out.println("client: del reqId = " + mRequestId);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            public void process(ClosedPositionReport aClosedPositionReport) {
                super.process(aClosedPositionReport);
                if (aClosedPositionReport.getFXCMCloseClOrdID().equals(mRequestId)) {
                    setSuccess(true);
                }
            }

            public void process(CollateralReport aCollateralReport) {
                try {
                    if (mOrder) {
                        mOrder = false;
                        OrderSingle orderSingle = MessageGenerator.generateMarketOrder(
                                aCollateralReport.getAccount(),
                                aCollateralReport.getQuantity(),
                                SideFactory.BUY,
                                "EUR/USD",
                                cem);
                        mRequestId = mFxcmGateway.sendMessage(orderSingle);
                        System.out.println("client: good order requestid = " + mRequestId);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            public void messageArrived(ITransportable aMessage) {
                handleMessage(aMessage, mAccounts, this);
            }
        }

        return doResult(new GenericListener());
    }

    public boolean testCloseTrueMarketOrder() {
        final String cem = Util.getCurrentlyExecutingMethod();
        System.out.println(cem);
        class GenericListener extends MessageTestHandler {
            private List mAccounts = new ArrayList();
            private boolean mOrder = true;
            private boolean mFlag = true;
            private String mRequestId;

            public void process(PositionReport aPositionReport) {
                System.out.println("client: incoming pos report = " + aPositionReport);
                if (safeEquals(mRequestId, aPositionReport.getRequestID())) {
                    if (mFlag) {
                        try {
                            ISide side;
                            // flip the side and set the correct price
                            if (aPositionReport.getPositionQty().getSide() == SideFactory.BUY) {
                                side = SideFactory.SELL;
                            } else {
                                side = SideFactory.BUY;
                            }
                            mFlag = false;
                            System.out.println("client: closing the open position in 2 seconds.");
                            // set to the opposite side
                            OrderSingle delOrder = MessageGenerator.generateCloseMarketOrder(
                                    aPositionReport.getFXCMPosID(),
                                    aPositionReport.getAccount(),
                                    aPositionReport.getPositionQty().getQty(),
                                    side,
                                    aPositionReport.getInstrument().getSymbol(),
                                    cem);
                            mRequestId = mFxcmGateway.sendMessage(delOrder);
                            System.out.println("client: del reqId = " + mRequestId);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            public void process(ClosedPositionReport aClosedPositionReport) {
                super.process(aClosedPositionReport);
                if (aClosedPositionReport.getFXCMCloseClOrdID().equals(mRequestId)) {
                    setSuccess(true);
                }
            }

            public void process(CollateralReport aCollateralReport) {
                try {
                    if (mOrder) {
                        mOrder = false;
                        OrderSingle orderSingle = MessageGenerator.generateMarketOrder(
                                aCollateralReport.getAccount(),
                                aCollateralReport.getQuantity(),
                                SideFactory.BUY,
                                "EUR/USD",
                                cem);
                        mRequestId = mFxcmGateway.sendMessage(orderSingle);
                        System.out.println("client: good order requestid = " + mRequestId);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            public void messageArrived(ITransportable aMessage) {
                handleMessage(aMessage, mAccounts, this);
            }
        }

        return doResult(new GenericListener());
    }

    public boolean testCreateEntryOrder(final boolean aIsDay) {
        final String cem = Util.getCurrentlyExecutingMethod();
        System.out.println(cem);
        class GenericListener extends MessageTestHandler {
            private List mAccounts = new ArrayList();
            private boolean mOrder = true;
            private String mRequestId;
            private boolean mIsDay = aIsDay;

            public void process(CollateralReport aCollateralReport) {
                super.process(aCollateralReport);
                try {
                    if (mOrder) {
                        mOrder = false;
                        /*
                        OrderSingle os = MessageGenerator.generateStopLimitEntry(
                                1.1,
                                OrdTypeFactory.STOP,
                                aCollateralReport.getAccount(),
                                aCollateralReport.getQuantity(),
                                SideFactory.SELL,
                                "EUR/USD",
                                cem);
                        if (mIsDay) {
                            os.setTimeInForce(TimeInForceFactory.DAY);
                        }
                        mRequestId = mFxcmGateway.sendMessage(os);
                        System.out.println("client: entry order requestId = " + mRequestId);
                        */

                        OrderSingle os2 = MessageGenerator.generateStopLimitEntry(
                                1.1,
                                OrdTypeFactory.LIMIT,
                                aCollateralReport.getAccount(),
                                aCollateralReport.getQuantity(),
                                SideFactory.SELL,
                                "EUR/USD",
                                cem);
                        if (mIsDay) {
                            os2.setTimeInForce(TimeInForceFactory.DAY);
                        }
                        mRequestId = mFxcmGateway.sendMessage(os2);
                        System.out.println("client: entry order requestId = " + mRequestId);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            public void process(ExecutionReport aExecutionReport) {
                super.process(aExecutionReport);
                if (safeEquals(aExecutionReport.getRequestID(), mRequestId)) {
                    setSuccess(true);
                }
            }

            public void messageArrived(ITransportable aMessage) {
                handleMessage(aMessage, mAccounts, this);
            }
        }

        return doResult(new GenericListener());
    }

    public boolean testCreateTrailingEntryOrder() {
        final String cem = Util.getCurrentlyExecutingMethod();
        System.out.println(cem);
        class GenericListener extends MessageTestHandler {
            private List mAccounts = new ArrayList();
            private boolean mOrder = true;
            private String mRequestId;

            public void process(CollateralReport aCollateralReport) {
                super.process(aCollateralReport);
                try {
                    if (mOrder) {
                        mOrder = false;
                        OrderSingle os = MessageGenerator.generateStopLimitEntry(
                                1.1,
                                OrdTypeFactory.STOP,
                                aCollateralReport.getAccount(),
                                aCollateralReport.getQuantity(),
                                SideFactory.SELL,
                                "EUR/USD",
                                cem,
                                10);
                        mRequestId = mFxcmGateway.sendMessage(os);
                        System.out.println("client: entry order requestId = " + mRequestId);

                        OrderSingle os2 = MessageGenerator.generateStopLimitEntry(
                                1.5,
                                OrdTypeFactory.LIMIT,
                                aCollateralReport.getAccount(),
                                aCollateralReport.getQuantity(),
                                SideFactory.SELL,
                                "EUR/USD",
                                cem,
                                10);
                        mRequestId = mFxcmGateway.sendMessage(os2);
                        System.out.println("client: entry order requestId = " + mRequestId);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            public void process(ExecutionReport aExecutionReport) {
                super.process(aExecutionReport);
                if (safeEquals(aExecutionReport.getRequestID(), mRequestId)) {
                    setSuccess(true);
                }
            }

            public void messageArrived(ITransportable aMessage) {
                handleMessage(aMessage, mAccounts, this);
            }
        }

        return doResult(new GenericListener());
    }

    public boolean testCreatePreviouslyQuotedOrder() {
        final String cem = Util.getCurrentlyExecutingMethod();
        System.out.println(cem);
        class GenericListener extends MessageTestHandler {
            private List mAccounts = new ArrayList();
            private boolean mOrder = true;
            private String mRequestId;

            public void process(MarketDataSnapshot aMarketDataSnapshot) {
                try {
                    String symbol = aMarketDataSnapshot.getInstrument().getSymbol();
                    if (mOrder && !mAccounts.isEmpty() && symbol.indexOf("JPY") == -1) {
                        System.out.println("client: incoming mds = " + aMarketDataSnapshot);
                        mOrder = false;
                        CollateralReport acct = (CollateralReport) mAccounts.get(0);
                        OrderSingle orderSingle = MessageGenerator.generateOpenOrder(
                                aMarketDataSnapshot.getQuoteID(),
                                aMarketDataSnapshot.getAskClose(),
                                acct.getAccount(),
                                acct.getQuantity(),
                                SideFactory.BUY,
                                aMarketDataSnapshot.getInstrument().getSymbol(),
                                cem);
                        orderSingle.setTimeInForce(TimeInForceFactory.FILL_OR_KILL);
                        mRequestId = mFxcmGateway.sendMessage(orderSingle);
                        System.out.println("client: good order requestid = " + mRequestId);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            public void process(PositionReport aPositionReport) {
                super.process(aPositionReport);
                if (safeEquals(aPositionReport.getRequestID(), mRequestId)) {
                    setSuccess(true);
                }
            }

            public void messageArrived(ITransportable aMessage) {
                handleMessage(aMessage, mAccounts, this);
            }
        }

        return doResult(new GenericListener());
    }

    public boolean testCreateTrueMarketOrder() {
        String cem = Util.getCurrentlyExecutingMethod();
        System.out.println(cem);
        class GenericListener extends MessageTestHandler {
            private List mAccounts = new ArrayList();
            private boolean mOrder = true;
            private String mRequestId;

            public void process(CollateralReport aCollateralReport) {
                super.process(aCollateralReport);
                if (mOrder) {
                    mOrder = false;
                    OrderSingle orderSingle = MessageGenerator.generateMarketOrder(
                            aCollateralReport.getAccount(),
                            aCollateralReport.getQuantity(),
                            SideFactory.SELL,
                            "EUR/USD",
                            "true market order test");
                    orderSingle.setTimeInForce(TimeInForceFactory.FILL_OR_KILL);
                    try {
                        mRequestId = mFxcmGateway.sendMessage(orderSingle);
                        System.out.println("client: good order requestid = " + mRequestId);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            public void process(PositionReport aPositionReport) {
                super.process(aPositionReport);
                if (safeEquals(aPositionReport.getRequestID(), mRequestId)) {
                    setSuccess(true);
                }
            }

            public void messageArrived(ITransportable aMessage) {
                handleMessage(aMessage, mAccounts, this);
            }
        }

        return doResult(new GenericListener());
    }

    public boolean testNetQuantityClose() {
        String cem = Util.getCurrentlyExecutingMethod();
        System.out.println(cem);
        class GenericListener extends MessageTestHandler {
            private List mAccounts = new ArrayList();
            private boolean mOrder = true;
            private String mRequestId;
            private String mCloseRequestId;

            public void process(CollateralReport aCollateralReport) {
                super.process(aCollateralReport);
                if (mOrder) {
                    mOrder = false;
                    try {
                        for (int i = 0; i < 5; i++) {
                            mRequestId = mFxcmGateway.sendMessage(MessageGenerator.generateMarketOrder(
                                    aCollateralReport.getAccount(),
                                    aCollateralReport.getQuantity(),
                                    SideFactory.BUY,
                                    "EUR/USD",
                                    "NQTEST-OPEN" + i));
                            System.out.println("client: open order requestid = " + mRequestId);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            public void process(ExecutionReport aExecutionReport) {
                super.process(aExecutionReport);
                if (safeEquals(aExecutionReport.getClOrdID(), mCloseRequestId)) {
                    if (aExecutionReport.getOrdStatus() == OrdStatusFactory.FILLED) {
                        setSuccess(true);
                    }
                }
            }

            public void process(PositionReport aPositionReport) {
                super.process(aPositionReport);
                if (safeEquals(aPositionReport.getRequestID(), mRequestId)) {
                    OrderSingle orderSingle = MessageGenerator.generateMarketOrder(
                            aPositionReport.getAccount(),
                            0,
                            SideFactory.SELL,
                            "EUR/USD",
                            "NQTEST-CLOSE");
                    orderSingle.setOrderPercent(100);
                    try {
                        mRequestId = null;
                        mCloseRequestId = mFxcmGateway.sendMessage(orderSingle);
                        System.out.println("client: close order requestid = " + mCloseRequestId);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            }

            public void messageArrived(ITransportable aMessage) {
                handleMessage(aMessage, mAccounts, this);
            }
        }

        return doResult(new GenericListener());
    }

    public boolean testNetQuantityStopLimit() {
        String cem = Util.getCurrentlyExecutingMethod();
        System.out.println(cem);
        class GenericListener extends MessageTestHandler {
            private List mAccounts = new ArrayList();
            private boolean mOrder = true;
            private String mRequestId;

            public void process(CollateralReport aCollateralReport) {
                super.process(aCollateralReport);
                if (mOrder && !mAccounts.isEmpty()) {
                    mOrder = false;
                    OrderSingle orderSingle = MessageGenerator.generateMarketOrder(
                            aCollateralReport.getAccount(),
                            aCollateralReport.getQuantity(),
                            SideFactory.BUY,
                            "EUR/USD",
                            "true market order test");
                    try {
                        mRequestId = mFxcmGateway.sendMessage(orderSingle);
                        System.out.println("client: good order requestid = " + mRequestId);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            public void process(ExecutionReport aExecutionReport) {
                super.process(aExecutionReport);
                if (safeEquals(mRequestId, aExecutionReport.getRequestID())) {
                    setSuccess(true);
                }
            }

            public void process(PositionReport aPositionReport) {
                super.process(aPositionReport);
                if (safeEquals(mRequestId, aPositionReport.getRequestID())) {
                    System.out.println("client: net qty stop/limit requestId = " + mRequestId);
                    CollateralReport acct = (CollateralReport) mAccounts.get(0);
                    OrderList ol = new OrderList();

                    OrderSingle stop = MessageGenerator.generateStopLimitEntry(
                            1.1,
                            OrdTypeFactory.STOP,
                            acct.getAccount(),
                            0,
                            SideFactory.SELL,
                            "EUR/USD",
                            "entry order - contingency - stop loss");
                    stop.setOrderPercent(100); //xxx this indicates the order is netquantity
                    ol.addOrder(stop);

                    OrderSingle limit = MessageGenerator.generateStopLimitEntry(
                            1.5,
                            OrdTypeFactory.LIMIT,
                            acct.getAccount(),
                            0,
                            SideFactory.SELL,
                            "EUR/USD",
                            "entry order - contingency - limit profit");
                    limit.setOrderPercent(100); //xxx this indicates the order is netquantity
                    ol.addOrder(limit);

                    try {
                        mRequestId = mFxcmGateway.sendMessage(ol);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            public void messageArrived(ITransportable aMessage) {
                handleMessage(aMessage, mAccounts, this);
            }
        }

        return doResult(new GenericListener());
    }

    public boolean testDeleteEntryOrder() {
        final String cem = Util.getCurrentlyExecutingMethod();
        System.out.println(cem);
        class GenericListener extends MessageTestHandler {
            private List mAccounts = new ArrayList();
            private boolean mOrder = true;
            private String mRequestId;
            private boolean mOpen = true;

            public void process(ExecutionReport aExe) {
                super.process(aExe);
                System.out.println("client: inc exe rpt = " + aExe);
                if (MessageAnalyzer.isStopLimitEntryOrder(aExe) && safeEquals(mRequestId, aExe.getRequestID())) {
                    try {
                        if (!aExe.getFXCMOrdStatus().isDeleted() && mOpen) {
                            mOpen = false;
                            System.out.println("client: got an entry order, waiting 2 seconds.");
                            OrderCancelRequest ocr = new OrderCancelRequest();
                            System.out.println("client: deleting the entry order");
                            ocr.setSecondaryClOrdID("text custom text delete order");
                            ocr.setOrderID(aExe.getOrderID());
                            ocr.setOrigClOrdID(aExe.getClOrdID());
                            ocr.setSide(aExe.getSide());
                            ocr.setInstrument(aExe.getInstrument());
                            ocr.setAccount(aExe.getAccount());
                            ocr.setOrderQty(aExe.getOrderQty());
                            mRequestId = mFxcmGateway.sendMessage(ocr);
                            System.out.println("client: del reqId = " + mRequestId);
                        } else if (aExe.getFXCMOrdStatus().isDeleted()) {
                            setSuccess(true);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            public void process(MarketDataSnapshot aMarketDataSnapshot) {
                super.process(aMarketDataSnapshot);
                try {
                    String symbol = aMarketDataSnapshot.getInstrument().getSymbol();
                    //choose the first non japanese mds for demonstration purposes
                    if (mOrder && !mAccounts.isEmpty() && symbol.indexOf("JPY") == -1) {
                        mOrder = false;
                        CollateralReport acct = (CollateralReport) mAccounts.get(0);
                        OrderSingle orderSingle = MessageGenerator.generateStopLimitEntry(
                                aMarketDataSnapshot.getAskClose() + .0080,
                                OrdTypeFactory.STOP,
                                acct.getAccount(),
                                acct.getQuantity(),
                                SideFactory.BUY,
                                aMarketDataSnapshot.getInstrument().getSymbol(),
                                cem);
                        mRequestId = mFxcmGateway.sendMessage(orderSingle);
                        System.out.println("client: entry order requestId = " + mRequestId);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            public void messageArrived(ITransportable aMessage) {
                handleMessage(aMessage, mAccounts, this);
            }
        }

        return doResult(new GenericListener());
    }

    public boolean testDeleteSLEntryOrder() {
        final String cem = Util.getCurrentlyExecutingMethod();
        System.out.println(cem);
        class GenericListener extends MessageTestHandler {
            private List mAccounts = new ArrayList();
            private boolean mOrder = true;
            private String mLimitId;
            private String mStopId;
            private String mRequestId;
            private boolean mGotLimitDelete;
            private boolean mGotStoptDelete;

            public void process(ExecutionReport aExe) {
                super.process(aExe);
                try {
                    if (MessageAnalyzer.isStopLimitEntryOrder(aExe) && safeEquals(mRequestId, aExe.getRequestID())) {
                        System.out.println("\nclient: trying to set a stop on the entry order");
                        OrderSingle order = MessageGenerator.generateStopLimitClose(
                                aExe.getPrice() - .0080,
                                aExe.getFXCMPosID(),
                                OrdTypeFactory.STOP,
                                aExe.getAccount(),
                                aExe.getOrderQty(),
                                SideFactory.BUY,
                                aExe.getInstrument().getSymbol(),
                                cem);
                        mStopId = mFxcmGateway.sendMessage(order);
                        System.out.println("client: stop  order reqId = " + mStopId);

                        System.out.println("\nclient: trying to set a limit on the entry order");
                        order = MessageGenerator.generateStopLimitClose(
                                aExe.getPrice() + .0080,
                                aExe.getFXCMPosID(),
                                OrdTypeFactory.LIMIT,
                                aExe.getAccount(),
                                aExe.getOrderQty(),
                                SideFactory.BUY,
                                aExe.getInstrument().getSymbol(),
                                cem);
                        mLimitId = mFxcmGateway.sendMessage(order);
                        System.out.println("client: limit order reqId = " + mLimitId);
                    } else if (MessageAnalyzer.isStopLimitCloseOrder(aExe) && (
                            safeEquals(mStopId, aExe.getRequestID())
                            || safeEquals(mLimitId, aExe.getRequestID()))) {
                        if (aExe.getFXCMOrdStatus().isDeleted()) {
                            if (MessageAnalyzer.isLimitOrder(aExe)) {
                                mGotLimitDelete = true;
                                if (mGotStoptDelete) {
                                    setSuccess(true);
                                }
                            } else {
                                mGotStoptDelete = true;
                                if (mGotLimitDelete) {
                                    setSuccess(true);
                                }
                            }
                        } else {
                            if (MessageAnalyzer.isLimitOrder(aExe)) {
                                System.out.println("\nclient: deleting a limit order");
                                OrderCancelRequest ocr =
                                        MessageGenerator.generateOrderCancelRequest(
                                                "test custom text delete limit",
                                                aExe.getOrderID(),
                                                aExe.getSide(),
                                                aExe.getAccount());
                                ocr.setOrderID(aExe.getOrderID());
                                ocr.setOrigClOrdID(aExe.getClOrdID());
                                ocr.setSide(aExe.getSide());
                                ocr.setInstrument(aExe.getInstrument());
                                ocr.setAccount(aExe.getAccount());
                                ocr.setOrderQty(aExe.getOrderQty());
                                mLimitId = mFxcmGateway.sendMessage(ocr);
                            } else {
                                System.out.println("\nclient: deleting a stop order");
                                OrderCancelRequest ocr =
                                        MessageGenerator.generateOrderCancelRequest(
                                                "test custom text delete stop",
                                                aExe.getOrderID(),
                                                aExe.getSide(),
                                                aExe.getAccount());
                                ocr.setOrderID(aExe.getOrderID());
                                ocr.setOrigClOrdID(aExe.getClOrdID());
                                ocr.setSide(aExe.getSide());
                                ocr.setInstrument(aExe.getInstrument());
                                ocr.setAccount(aExe.getAccount());
                                ocr.setOrderQty(aExe.getOrderQty());
                                mStopId = mFxcmGateway.sendMessage(ocr);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            public void process(MarketDataSnapshot aMarketDataSnapshot) {
                super.process(aMarketDataSnapshot);
                try {
                    String symbol = aMarketDataSnapshot.getInstrument().getSymbol();
                    //choose the first non japanese mds for demonstration purposes
                    if (mOrder && !mAccounts.isEmpty() && symbol.indexOf("JPY") == -1) {
                        mOrder = false;
                        CollateralReport acct = (CollateralReport) mAccounts.get(0);
                        OrderSingle orderSingle = MessageGenerator.generateStopLimitEntry(
                                aMarketDataSnapshot.getAskClose() + .0080,
                                OrdTypeFactory.STOP,
                                acct.getAccount(),
                                acct.getQuantity(),
                                SideFactory.BUY,
                                aMarketDataSnapshot.getInstrument().getSymbol(),
                                cem);
                        mRequestId = mFxcmGateway.sendMessage(orderSingle);
                        System.out.println("client: good order requestid = " + mRequestId);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            public void messageArrived(ITransportable aMessage) {
                handleMessage(aMessage, mAccounts, this);
            }
        }

        return doResult(new GenericListener());
    }

    public boolean testDeleteSLMarketOrder() {
        final String cem = Util.getCurrentlyExecutingMethod();
        System.out.println(cem);
        class GenericListener extends MessageTestHandler {
            private List mAccounts = new ArrayList();
            private boolean mOrder = true;
            private String mRequestId;
            private String mLimitId;
            private String mStopId;
            private boolean mGotLimitDelete;
            private boolean mGotStoptDelete;

            public void process(ExecutionReport aExe) {
                super.process(aExe);
                if (safeEquals(mStopId, aExe.getRequestID()) || safeEquals(mLimitId, aExe.getRequestID())) {
                    System.out.println("client: inc exe rpt = " + aExe);
                    try {
                        if (MessageAnalyzer.isStopLimitCloseOrder(aExe)) {
                            if (aExe.getFXCMOrdStatus().isDeleted()) {
                                if (MessageAnalyzer.isLimitOrder(aExe)) {
                                    mGotLimitDelete = true;
                                    if (mGotStoptDelete) {
                                        setSuccess(true);
                                    }
                                } else {
                                    mGotStoptDelete = true;
                                    if (mGotLimitDelete) {
                                        setSuccess(true);
                                    }
                                }
                            } else {
                                if (MessageAnalyzer.isLimitOrder(aExe)) {
                                    System.out.println("client: deleting a limit order");
                                    // this sleep is not neccessary it is just here for illustration
                                    OrderCancelRequest ocr =
                                            MessageGenerator.generateOrderCancelRequest(
                                                    "test custom text delete limit",
                                                    aExe.getOrderID(),
                                                    aExe.getSide(),
                                                    aExe.getAccount());
                                    ocr.setOrderID(aExe.getOrderID());
                                    ocr.setOrigClOrdID(aExe.getClOrdID());
                                    ocr.setSide(aExe.getSide());
                                    ocr.setInstrument(aExe.getInstrument());
                                    ocr.setAccount(aExe.getAccount());
                                    ocr.setOrderQty(aExe.getOrderQty());
                                    mLimitId = mFxcmGateway.sendMessage(ocr);
                                } else {
                                    System.out.println("client: deleting a stop order");
                                    // this sleep is not neccessary it is just here for illustration
                                    OrderCancelRequest ocr =
                                            MessageGenerator.generateOrderCancelRequest(
                                                    "test custom text delete stop",
                                                    aExe.getOrderID(),
                                                    aExe.getSide(),
                                                    aExe.getAccount());
                                    ocr.setOrderID(aExe.getOrderID());
                                    ocr.setOrigClOrdID(aExe.getClOrdID());
                                    ocr.setSide(aExe.getSide());
                                    ocr.setInstrument(aExe.getInstrument());
                                    ocr.setAccount(aExe.getAccount());
                                    ocr.setOrderQty(aExe.getOrderQty());
                                    mStopId = mFxcmGateway.sendMessage(ocr);
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            public void process(CollateralReport aCollateralReport) {
                try {
                    if (mOrder) {
                        mOrder = false;
                        OrderSingle orderSingle = MessageGenerator.generateMarketOrder(
                                aCollateralReport.getAccount(),
                                aCollateralReport.getQuantity(),
                                SideFactory.BUY,
                                "EUR/USD",
                                cem);
                        mRequestId = mFxcmGateway.sendMessage(orderSingle);
                        System.out.println("client: good order requestid = " + mRequestId);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            public void process(PositionReport aPositionReport) {
                super.process(aPositionReport);
                if (safeEquals(mRequestId, aPositionReport.getRequestID())) {
                    System.out.println("client: inc pos report = " + aPositionReport);
                    try {
                        System.out.println("\nclient: trying to set a stop on the open position");
                        OrderSingle order = MessageGenerator.generateStopLimitClose(
                                aPositionReport.getSettlPrice() - .0080,
                                aPositionReport.getFXCMPosID(),
                                OrdTypeFactory.STOP,
                                aPositionReport.getAccount(),
                                aPositionReport.getPositionQty().getQty(),
                                SideFactory.BUY,
                                aPositionReport.getInstrument().getSymbol(),
                                cem);
                        mStopId = mFxcmGateway.sendMessage(order);
                        System.out.println("client: stop  order reqId = " + mStopId + "\n\n");

                        System.out.println("\nclient: trying to set a limit on the open position");
                        order = MessageGenerator.generateStopLimitClose(
                                aPositionReport.getSettlPrice() + .0080,
                                aPositionReport.getFXCMPosID(),
                                OrdTypeFactory.LIMIT,
                                aPositionReport.getAccount(),
                                aPositionReport.getPositionQty().getQty(),
                                SideFactory.BUY,
                                aPositionReport.getInstrument().getSymbol(),
                                cem);
                        mLimitId = mFxcmGateway.sendMessage(order);
                        System.out.println("client: limit order reqId = " + mLimitId);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            public void messageArrived(ITransportable aMessage) {
                handleMessage(aMessage, mAccounts, this);
            }
        }

        return doResult(new GenericListener());
    }

    public boolean testHistoricalSnapshot() {
        String cem = Util.getCurrentlyExecutingMethod();
        System.out.println(cem);
        class GenericListener extends MessageTestHandler {
            private Date mStartDate;
            private String mClosedID;
            private UTCDate mFxcmStartDate;
            private UTCTimeOnly mFxcmStartTime;
            private int mFxcmMaxNoResults = 300;
            private int mTotalReports;

            GenericListener() {
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.DATE, -1); //roll back 3 days
                System.out.println("calendar.getTime() = " + calendar.getTime());
                mStartDate = new GregorianCalendar(
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DATE), 0, 0, 0).getTime();
                mFxcmStartDate = new UTCDate(mStartDate);
                mFxcmStartTime = new UTCTimeOnly(mStartDate);
            }

            public void process(RequestForPositionsAck aRequestForPositionsAck) {
                if (safeEquals(mClosedID, aRequestForPositionsAck.getRequestID())) {
                    mTotalReports += aRequestForPositionsAck.getTotalNumPosReports();
                    System.out.println("aRequestForPositionsAck.getTotalNumPosReports() = " + aRequestForPositionsAck.getTotalNumPosReports());
                    if (aRequestForPositionsAck.getTotalNumPosReports() == 0) {
                        setSuccess(true);
                        System.out.println("Total Reports Received = " + mTotalReports);
                    }
                }
            }

            public void process(TradingSessionStatus aTradingSessionStatus) {
                super.process(aTradingSessionStatus);
                Calendar calendar = Calendar.getInstance();
                Date date = calendar.getTime();
                UTCDate fxcmEndDate = new UTCDate(date);
                UTCTimeOnly fxcmEndTime = new UTCTimeOnly(date);
                System.out.println("fxcmStartDate = " + mFxcmStartDate);
                System.out.println("fxcmStartTime = " + mFxcmStartTime);
                System.out.println("fxcmEndDate = " + fxcmEndDate);
                System.out.println("fxcmEndTime = " + fxcmEndTime);
                mClosedID = mFxcmGateway.requestClosedPositions(mFxcmMaxNoResults, mFxcmStartDate, mFxcmStartTime, fxcmEndDate, fxcmEndTime);
                System.out.println("closedID = " + mClosedID);
            }

            public void process(ClosedPositionReport aClosedPositionReport) {
                if (safeEquals(mClosedID, aClosedPositionReport.getRequestID())) {
                    if (aClosedPositionReport.isLastRptRequested()) {
                        super.process(aClosedPositionReport);
                        UTCDate fxcmEndDate = new UTCDate(aClosedPositionReport.getFXCMPosCloseTime());
                        UTCTimeOnly fxcmEndTime = new UTCTimeOnly(aClosedPositionReport.getFXCMPosCloseTime());
                        System.out.println("fxcmStartDate = " + mFxcmStartDate);
                        System.out.println("fxcmStartTime = " + mFxcmStartTime);
                        System.out.println("fxcmEndDate = " + fxcmEndDate);
                        System.out.println("fxcmEndTime = " + fxcmEndTime);
                        mClosedID = mFxcmGateway.requestClosedPositions(mFxcmMaxNoResults, mFxcmStartDate, mFxcmStartTime, fxcmEndDate, fxcmEndTime);
                    }
                }
            }

            public void messageArrived(ITransportable aMessage) {
                handleMessage(aMessage, null, this);
            }
        }

        return doResult(new GenericListener());
    }

    public boolean testInterestRateChange() {
        String cem = Util.getCurrentlyExecutingMethod();
        System.out.println(cem);
        class GenericListener extends MessageTestHandler {
            public void process(SecurityStatus aSecurityStatus) {
                super.process(aSecurityStatus);
            }

            public void messageArrived(ITransportable aMessage) {
                handleMessage(aMessage, null, this);
            }
        }

        return doResult(new GenericListener());
    }

    public boolean testListen() {
        String cem = Util.getCurrentlyExecutingMethod();
        System.out.println(cem);
        class GenericListener extends MessageTestHandler {
            public void process(MarketDataSnapshot aMarketDataSnapshot) {
                super.process(aMarketDataSnapshot);
                System.out.println("client inc: aMarketDataSnapshot = " + aMarketDataSnapshot);
            }

            public void messageArrived(ITransportable aMessage) {
                handleMessage(aMessage, null, this);
            }
        }

        return doResult(new GenericListener());
    }

    public boolean testRequestEquity() {
        String cem = Util.getCurrentlyExecutingMethod();
        System.out.println(cem);
        class GenericListener extends MessageTestHandler {
            public void process(CollateralReport aCollateralReport) {
                if (safeEquals(mAccountMassID, aCollateralReport.getRequestID())) {
                    System.out.println("aCollateralReport.getEndCash() = " + aCollateralReport.getEndCash());
                    mFxcmGateway.requestAccountByName(aCollateralReport.getAccount());
                } else if (aCollateralReport.getRequestID() != null) {
                    System.out.println("aCollateralReport.getEndCash() = " + aCollateralReport.getEndCash());
                } else {
                    mFxcmGateway.requestAccountByName(aCollateralReport.getAccount());
                }
            }

            public void messageArrived(ITransportable aMessage) {
                handleMessage(aMessage, null, this);
            }
        }
        return doResult(new GenericListener());
    }

    public boolean testMarketDataRequest() {
        String cem = Util.getCurrentlyExecutingMethod();
        System.out.println(cem);
        class GenericListener extends MessageTestHandler {
            private String mMarketDataRequestID;

            public void process(TradingSessionStatus aTradingSessionStatus) {
                try {
                    MarketDataRequest mdr = new MarketDataRequest();
                    Enumeration securities = aTradingSessionStatus.getSecurities();
                    while (securities.hasMoreElements()) {
                        TradingSecurity o = (TradingSecurity) securities.nextElement();
                        mdr.addRelatedSymbol(o);
                    }
                    mdr.setSubscriptionRequestType(SubscriptionRequestTypeFactory.SUBSCRIBE);
                    mdr.setMDEntryTypeSet(MarketDataRequest.MDENTRYTYPESET_ALL);
                    mMarketDataRequestID = mFxcmGateway.sendMessage(mdr);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            public void process(MarketDataSnapshot aMarketDataSnapshot) {
                super.process(aMarketDataSnapshot);
                if (safeEquals(aMarketDataSnapshot.getRequestID(), mMarketDataRequestID)) {
                    System.out.println("client inc: aMarketDataSnapshot = " + aMarketDataSnapshot);
                    if (aMarketDataSnapshot.getFXCMContinuousFlag() == IFixDefs.FXCMCONTINUOUS_END) {
                        setSuccess(true);
                    }
                }
            }

            public void messageArrived(ITransportable aMessage) {
                handleMessage(aMessage, null, this);
            }
        }

        return doResult(new GenericListener());
    }

    public boolean testMarketDataHistory() {
        String cem = Util.getCurrentlyExecutingMethod();
        System.out.println(cem);
        class GenericListener extends MessageTestHandler {
            private UTCDate mStartDate;
            private UTCTimeOnly mStartTime;
            private int mCount;
            private UTCTimestamp mOpenTimestamp;

            GenericListener() {
                Calendar instance = Calendar.getInstance();
                instance.roll(Calendar.HOUR_OF_DAY, -1);
                mStartDate = new UTCDate(instance.getTime());
                mStartTime = new UTCTimeOnly(instance.getTime());

            }

            public void process(TradingSessionStatus aTradingSessionStatus) {
                super.process(aTradingSessionStatus);
                try {
                    MarketDataRequest mdr = new MarketDataRequest();
                    mdr.setSubscriptionRequestType(SubscriptionRequestTypeFactory.SNAPSHOT);
                    mdr.setResponseFormat(IFixDefs.MSGTYPE_FXCMRESPONSE);
                    mdr.setFXCMTimingInterval(FXCMTimingIntervalFactory.TICK);
                    mdr.setMDEntryTypeSet(MarketDataRequest.MDENTRYTYPESET_ALL);
                    mdr.setFXCMStartDate(mStartDate);
                    mdr.setFXCMStartTime(mStartTime);
                    mdr.addRelatedSymbol(mTradingSessionStatus.getSecurity("EUR/USD"));
                    mFxcmGateway.sendMessage(mdr);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            public void process(MarketDataRequestReject aMarketDataRequestReject) {
                System.out.println("aMarketDataRequestReject = " + aMarketDataRequestReject);
                setSuccess(true);
            }

            public void process(MarketDataSnapshot aMarketDataSnapshot) {
                super.process(aMarketDataSnapshot);
                if (aMarketDataSnapshot.getRequestID() != null) {
                    mCount++;
                    if (mOpenTimestamp == null) {
                        System.out.println(aMarketDataSnapshot);
                        mOpenTimestamp = aMarketDataSnapshot.getOpenTimestamp();
                        System.out.println("\n-----------------------------------");
                        System.out.println("first\t= " + aMarketDataSnapshot.getOpenTimestamp() + " = " + aMarketDataSnapshot.getRequestID());
                    }
                    if (aMarketDataSnapshot.getFXCMContinuousFlag() == IFixDefs.FXCMCONTINUOUS_END) {
                        System.out.println(aMarketDataSnapshot);
                        System.out.println("\nlast\t= " + aMarketDataSnapshot.getOpenTimestamp() + " = " + aMarketDataSnapshot.getRequestID());
                        MarketDataRequest mdr = new MarketDataRequest();
                        mdr.setSubscriptionRequestType(SubscriptionRequestTypeFactory.SNAPSHOT);
                        mdr.setResponseFormat(IFixDefs.MSGTYPE_FXCMRESPONSE);
                        mdr.setFXCMTimingInterval(FXCMTimingIntervalFactory.TICK);
                        mdr.setMDEntryTypeSet(MarketDataRequest.MDENTRYTYPESET_ALL);
                        mdr.setFXCMStartDate(mStartDate);
                        mdr.setFXCMStartTime(mStartTime);
                        mdr.setFXCMEndDate(new UTCDate(mOpenTimestamp));
                        mdr.setFXCMEndTime(new UTCTimeOnly(mOpenTimestamp));
                        mdr.addRelatedSymbol(mTradingSessionStatus.getSecurity("EUR/USD"));

                        System.out.println("FXCMStartDate\t= " + mdr.getFXCMStartDate());
                        System.out.println("FXCMStartTime\t= " + mdr.getFXCMStartTime());
                        System.out.println("FXCMEndDate\t\t= " + mdr.getFXCMEndDate());
                        System.out.println("FXCMEndTime\t\t= " + mdr.getFXCMEndTime());
                        mOpenTimestamp = null;
                        System.out.println("-----------------------------------\ntotal mds received = " + mCount);
                        System.out.println("\nout >> " + mdr);
                        try {
                            mFxcmGateway.sendMessage(mdr);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            public void messageArrived(ITransportable aMessage) {
                handleMessage(aMessage, null, this);
            }
        }

        return doResult(new GenericListener());
    }

    public boolean testOCO() {
        String cem = Util.getCurrentlyExecutingMethod();
        System.out.println(cem);
        class GenericListener extends MessageTestHandler {
            private String mRequestId;
            private List mOrderRequests = new ArrayList();
            private boolean mOrder = true;

            public void process(CollateralReport aCollateralReport) {
                super.process(aCollateralReport);
                if (mOrder) {
                    mOrder = false;
                    OrderList ol = new OrderList();
                    ol.setContingencyType(ContingencyTypeFactory.OCO);
                    OrderSingle stop = MessageGenerator.generateStopLimitEntry(
                            1.8,
                            OrdTypeFactory.STOP,
                            aCollateralReport.getAccount(),
                            aCollateralReport.getQuantity(),
                            SideFactory.BUY,
                            "GBP/USD",
                            "entry order - contingency - stop loss");
                    ol.addOrder(stop);
                    mOrderRequests.add(stop.getSecondaryClOrdID());

                    OrderSingle limit = MessageGenerator.generateStopLimitEntry(
                            1.1,
                            OrdTypeFactory.LIMIT,
                            aCollateralReport.getAccount(),
                            aCollateralReport.getQuantity(),
                            SideFactory.BUY,
                            "GBP/USD",
                            "entry order - contingency - limit profit");
                    ol.addOrder(limit);
                    mOrderRequests.add(limit.getSecondaryClOrdID());

                    try {
                        mRequestId = mFxcmGateway.sendMessage(ol);
                        System.out.println("client: submitting OCO order = " + mRequestId);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            public void process(ExecutionReport aExecutionReport) {
                if (mOrderRequests.contains(aExecutionReport.getSecondaryClOrdID())) {
                    System.out.println("CLIENT: OL ExecutionReport = " + aExecutionReport);
                    System.out.println("aExecutionReport.getFXCMContingencyID() = "
                                       + aExecutionReport.getFXCMContingencyID());
                    System.out.println("aExecutionReport.getListID() = " + aExecutionReport.getListID());
                    System.out.println("aExecutionReport.getRequestID() = " + aExecutionReport.getRequestID());
                    System.out.println("mOrderRequests = " + mOrderRequests);
                    mOrderRequests.remove(aExecutionReport.getSecondaryClOrdID());
                    if (mOrderRequests.isEmpty()) {
                        setSuccess(true);
                    }
                }
            }

            public void process(PositionReport aPositionReport) {
                if (mOrderRequests.contains(aPositionReport.getSecondaryClOrdID())) {
                    System.out.println("CLIENT: OL PositionReport = " + aPositionReport);
                    System.out.println("aPositionReport.getListID() = " + aPositionReport.getListID());
                    System.out.println("aPositionReport.getRequestID() = " + aPositionReport.getRequestID());
                    System.out.println("mOrderRequests = " + mOrderRequests);
                    mOrderRequests.remove(aPositionReport.getSecondaryClOrdID());
                }
            }

            public void messageArrived(ITransportable aMessage) {
                handleMessage(aMessage, null, this);
            }
        }

        return doResult(new GenericListener());
    }

    public boolean testComplexOCO() {
        String cem = Util.getCurrentlyExecutingMethod();
        System.out.println(cem);
        class GenericListener extends MessageTestHandler {
            private boolean mOrder = true;
            private String mNewContingencyID;
            private String mListID1;
            private String mListID2;
            private ExecutionReport mOrder1;
            private ExecutionReport mOrder2;
            private boolean mOCOSuccess;

            public void process(CollateralReport aCollateralReport) {
                super.process(aCollateralReport);
                // create 2 normal entry orders
                try {
                    if (mOrder) {
                        mOrder = false;
                        mListID1 = sendOrder(aCollateralReport);
                        System.out.println("client: good order requestid = " + mListID1);
                        mListID2 = sendOrder(aCollateralReport);
                        System.out.println("client: good order requestid = " + mListID2);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            private String sendOrder(CollateralReport aCollateralReport) {
                OrderList ol = new OrderList();
                ol.setContingencyType(ContingencyTypeFactory.ELS);
                OrderSingle primary = MessageGenerator.generateStopLimitEntry(
                        1.5,
                        OrdTypeFactory.STOP,
                        aCollateralReport.getAccount(),
                        aCollateralReport.getQuantity(),
                        SideFactory.BUY,
                        "EUR/USD",
                        "primary order");

                primary.setClOrdLinkID(IFixDefs.CLORDLINKID_PRIMARY);
                ol.addOrder(primary);

                OrderSingle stop = MessageGenerator.generateStopLimitEntry(
                        1.1,
                        OrdTypeFactory.STOP,
                        aCollateralReport.getAccount(),
                        aCollateralReport.getQuantity(),
                        SideFactory.SELL,
                        "EUR/USD",
                        "entry order - contingency - stop loss");
                stop.setClOrdLinkID(IFixDefs.CLORDLINKID_CONTINGENT);
                ol.addOrder(stop);

                OrderSingle limit = MessageGenerator.generateStopLimitEntry(
                        1.6,
                        OrdTypeFactory.LIMIT,
                        aCollateralReport.getAccount(),
                        aCollateralReport.getQuantity(),
                        SideFactory.SELL,
                        "EUR/USD",
                        "entry order - contingency - limit profit");
                limit.setClOrdLinkID(IFixDefs.CLORDLINKID_CONTINGENT);
                ol.addOrder(limit);

                try {
                    return mFxcmGateway.sendMessage(ol);
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }

            public void process(ExecutionReport aExecutionReport) {
                if (aExecutionReport.getListID().equals(mListID1)) {
                    if ("primary order".equals(aExecutionReport.getSecondaryClOrdID())) {
                        mOrder1 = aExecutionReport;
                        if (!mOCOSuccess) {
                            mNewContingencyID = aExecutionReport.getFXCMContingencyID();
                            if (mNewContingencyID == null) {
                                // make a new contingency group with order1
                                makeOCO(aExecutionReport);
                            } else {
                                // add order2 to order1's contingency group
                                makeOCO(mOrder2);
                            }
                        }
                    }
                }
                if (aExecutionReport.getListID().equals(mListID2)) {
                    if ("primary order".equals(aExecutionReport.getSecondaryClOrdID())) {
                        mOrder2 = aExecutionReport;
                    }
                }

                /*
                if (mOrder1 != null && mOrder2 != null) {
                    if (mOrder1.getFXCMContingencyID() != null && mOrder2.getFXCMContingencyID() != null) {
                        if (mOrder1.getFXCMContingencyID().equals(mOrder2.getFXCMContingencyID())) {
                            mOCOSuccess = true;
                            //oco was successfull, now remove from group
                            removeOCO(mOrder1);
                            removeOCO(mOrder2);
                        }
                    }
                    if (mOCOSuccess) {
                        if (mOrder1.getFXCMContingencyID() == null && mOrder2.getFXCMContingencyID() == null) {
                            //removal from oco group completed
                            setSuccess(true);
                        }
                    }
                }
                */
            }

            private void makeOCO(ExecutionReport aExecutionReport) {
                OrderCancelReplaceRequest os =
                        MessageGenerator.generateOrderReplaceRequest(
                                aExecutionReport.getText(),
                                aExecutionReport.getOrderID(),
                                aExecutionReport.getSide(),
                                aExecutionReport.getOrdType(),
                                aExecutionReport.getPrice(),
                                aExecutionReport.getAccount());
                os.setContingencyType(ContingencyTypeFactory.OCO);
                if (mNewContingencyID != null) {
                    os.setFXCMContingencyID(mNewContingencyID);
                }
                try {
                    mFxcmGateway.sendMessage(os);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            private void removeOCO(ExecutionReport aExecutionReport) {
                OrderCancelReplaceRequest os =
                        MessageGenerator.generateOrderReplaceRequest(
                                aExecutionReport.getText(),
                                aExecutionReport.getOrderID(),
                                aExecutionReport.getSide(),
                                aExecutionReport.getOrdType(),
                                aExecutionReport.getPrice(),
                                aExecutionReport.getAccount());
                if (mNewContingencyID != null) {
                    os.setFXCMContingencyID(mNewContingencyID);
                }
                try {
                    mFxcmGateway.sendMessage(os);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            public void messageArrived(ITransportable aMessage) {
                handleMessage(aMessage, null, this);
            }
        }

        return doResult(new GenericListener());
    }

    public boolean testOpenCloseMarket() {
        String cem = Util.getCurrentlyExecutingMethod();
        System.out.println(cem);
        class GenericListener extends MessageTestHandler {
            public void process(TradingSessionStatus aTradingSessionStatus) {
                super.process(aTradingSessionStatus);
            }

            public void messageArrived(ITransportable aMessage) {
                handleMessage(aMessage, null, this);
            }
        }

        return doResult(new GenericListener());
    }

    public boolean testOpenLimit() {
        final String cem = Util.getCurrentlyExecutingMethod();
        System.out.println(cem);
        class GenericListener extends MessageTestHandler {
            private List mAccounts = new ArrayList();
            private boolean mOrder = true;
            private String mRequestId;

            public void process(MarketDataSnapshot aMarketDataSnapshot) {
                try {
                    if (mOrder && !mAccounts.isEmpty()) {
                        System.out.println("client: incoming mds = " + aMarketDataSnapshot);
                        mOrder = false;
                        CollateralReport acct = (CollateralReport) mAccounts.get(0);
                        OrderSingle orderSingle = MessageGenerator.generateOpenOrder(
                                aMarketDataSnapshot.getAskClose(),
                                acct.getAccount(),
                                acct.getQuantity(),
                                SideFactory.BUY,
                                aMarketDataSnapshot.getInstrument().getSymbol(),
                                cem);
                        orderSingle.setOrdType(OrdTypeFactory.LIMIT);
                        orderSingle.setTimeInForce(TimeInForceFactory.FILL_OR_KILL);
                        mRequestId = mFxcmGateway.sendMessage(orderSingle);
                        System.out.println("client: good order requestid = " + mRequestId);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            public void process(PositionReport aPositionReport) {
                super.process(aPositionReport);
                if (safeEquals(aPositionReport.getRequestID(), mRequestId)) {
                    setSuccess(true);
                }
            }

            public void messageArrived(ITransportable aMessage) {
                handleMessage(aMessage, mAccounts, this);
            }
        }

        return doResult(new GenericListener());
    }

    public boolean testOpenRangeOrder() {
        final String cem = Util.getCurrentlyExecutingMethod();
        System.out.println(cem);
        class GenericListener extends MessageTestHandler {
            private List mAccounts = new ArrayList();
            private boolean mOrder = true;
            private String mRequestId;

            public void process(MarketDataSnapshot aMarketDataSnapshot) {
                try {
                    String symbol = aMarketDataSnapshot.getInstrument().getSymbol();
                    if (mOrder && !mAccounts.isEmpty() && symbol.indexOf("JPY") == -1) {
                        System.out.println("client: incoming mds = " + aMarketDataSnapshot);
                        mOrder = false;
                        CollateralReport acct = (CollateralReport) mAccounts.get(0);
                        OrderSingle orderSingle = MessageGenerator.generateOpenOrder(
                                0,
                                acct.getAccount(),
                                acct.getQuantity(),
                                SideFactory.BUY,
                                aMarketDataSnapshot.getInstrument().getSymbol(),
                                cem);
                        orderSingle.setOrdType(OrdTypeFactory.STOP_LIMIT);
                        double points = .0008;
                        double start = aMarketDataSnapshot.getAskClose() - points;
                        double stopPx = aMarketDataSnapshot.getAskClose() + points;
                        orderSingle.setPrice(start);
                        orderSingle.setStopPx(stopPx);
                        System.out.println("current price = " + aMarketDataSnapshot.getAskClose());
                        System.out.println("price = " + start);
                        System.out.println("stoppx = " + stopPx);
                        System.out.println("orderSingle.getPrice() = " + orderSingle.getPrice());
                        System.out.println("orderSingle.getStopPx() = " + orderSingle.getStopPx());
                        System.out.println("");
                        mRequestId = mFxcmGateway.sendMessage(orderSingle);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            public void process(PositionReport aPositionReport) {
                super.process(aPositionReport);
                if (safeEquals(aPositionReport.getRequestID(), mRequestId)) {
                    System.out.println("aPositionReport.getSettlPrice() = " + aPositionReport.getSettlPrice());
                    System.out.println("\n\n");
                    setSuccess(true);
                }
            }

            public void process(ExecutionReport aExecutionReport) {
                super.process(aExecutionReport);
            }

            public void messageArrived(ITransportable aMessage) {
                handleMessage(aMessage, mAccounts, this);
            }
        }

        return doResult(new GenericListener());
    }

    public boolean testAtMarketPointsOrder() {
        final String cem = Util.getCurrentlyExecutingMethod();
        System.out.println(cem);
        class GenericListener extends MessageTestHandler {
            private List mAccounts = new ArrayList();
            private boolean mOrder = true;
            private String mRequestId;

            public void process(MarketDataSnapshot aMarketDataSnapshot) {
                try {
                    if (mOrder && !mAccounts.isEmpty()) {
                        System.out.println("client: incoming mds = " + aMarketDataSnapshot);
                        mOrder = false;
                        CollateralReport acct = (CollateralReport) mAccounts.get(0);
                        OrderSingle orderSingle = MessageGenerator.generateOpenOrder(
                                aMarketDataSnapshot.getAskClose(),
                                acct.getAccount(),
                                acct.getQuantity(),
                                SideFactory.BUY,
                                aMarketDataSnapshot.getInstrument().getSymbol(),
                                cem,
                                100);
                        mRequestId = mFxcmGateway.sendMessage(orderSingle);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            public void process(PositionReport aPositionReport) {
                super.process(aPositionReport);
                if (safeEquals(aPositionReport.getRequestID(), mRequestId)) {
                    System.out.println("aPositionReport.getSettlPrice() = " + aPositionReport.getSettlPrice());
                    System.out.println("\n\n");
                    setSuccess(true);
                }
            }

            public void process(ExecutionReport aExecutionReport) {
                super.process(aExecutionReport);
                if (safeEquals(aExecutionReport.getRequestID(), mRequestId)) {
                    System.out.println("aExecutionReport.getPrice() = " + aExecutionReport.getPrice());
                }
            }

            public void messageArrived(ITransportable aMessage) {
                handleMessage(aMessage, mAccounts, this);
            }
        }

        return doResult(new GenericListener());
    }

    public boolean testOrderHistory() {
        String cem = Util.getCurrentlyExecutingMethod();
        System.out.println(cem);
        class GenericListener extends MessageTestHandler {
            private List mAccounts = new ArrayList();
            private boolean mOrder = true;
            private String mRequestId;
            private String mHistoryRequestId;

            public void process(CollateralReport aCollateralReport) {
                super.process(aCollateralReport);
                if (mOrder) {
                    mOrder = false;
                    OrderSingle orderSingle = MessageGenerator.generateMarketOrder(
                            aCollateralReport.getAccount(),
                            aCollateralReport.getQuantity(),
                            SideFactory.BUY,
                            "EUR/USD",
                            "true market order test");
                    try {
                        mRequestId = mFxcmGateway.sendMessage(orderSingle);
                        System.out.println("client: good order requestid = " + mRequestId);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            public void process(ExecutionReport aExecutionReport) {
                super.process(aExecutionReport);
                DecimalFormat df = new DecimalFormat();
                System.out.println("aExe = " + aExecutionReport);
                System.out.println("********************************************");
                System.out.println("exe.getOrderID() = " + aExecutionReport.getOrderID());
                System.out.println("exe.getOrderStatus() = " + aExecutionReport.getOrdStatus());
                System.out.println("exe.getFXCMPosID() = " + aExecutionReport.getFXCMPosID());
                System.out.println("exe.getAccount() = " + aExecutionReport.getAccount());
                System.out.println("exe.getTransactTime() = " + aExecutionReport.getTransactTime());
                System.out.println("exe.getRequestID() = " + aExecutionReport.getRequestID());
                System.out.println("exe.getExecType() = " + aExecutionReport.getExecType());
                System.out.println("exe.getFXCMOrdStatus() = " + aExecutionReport.getFXCMOrdStatus());
                System.out.println("exe.getOrdStatus() = " + aExecutionReport.getOrdStatus());
                System.out.println("exe.getPrice() = " + aExecutionReport.getPrice());
                System.out.println("exe.getOrderQty() = " + df.format(aExecutionReport.getOrderQty()));
                System.out.println("exe.getCumQty() = " + df.format(aExecutionReport.getCumQty()));
                System.out.println("exe.getLastQty() = " + df.format(aExecutionReport.getLastQty()));
                System.out.println("exe.getLeavesQty() = " + df.format(aExecutionReport.getLeavesQty()));
                System.out.println("exe.getOrderPercent() = " + aExecutionReport.getOrderPercent());
                System.out.println("********************************************\n\n");

                if (safeEquals(aExecutionReport.getRequestID(), mHistoryRequestId) && aExecutionReport.isLastRptRequested()) {
                    setSuccess(true);
                }
            }

            public void process(PositionReport aPositionReport) {
                super.process(aPositionReport);
                if (safeEquals(aPositionReport.getRequestID(), mRequestId)) {
                    OrderMassStatusRequest omsr = new OrderMassStatusRequest();
                    omsr.setAccount(aPositionReport.getAccount());
                    omsr.setOrderID(aPositionReport.getOrderID());
                    try {
                        mHistoryRequestId = mFxcmGateway.sendMessage(omsr);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            public void messageArrived(ITransportable aMessage) {
                handleMessage(aMessage, mAccounts, this);
            }
        }

        return doResult(new GenericListener());
    }

    public boolean testOrderList() {
        String cem = Util.getCurrentlyExecutingMethod();
        System.out.println(cem);
        class GenericListener extends MessageTestHandler {
            private List mAccounts = new ArrayList();
            private boolean mOrder = true;
            private String mRequestId;
            private List mOrderRequests = new ArrayList();

            public void process(CollateralReport aCollateralReport) {
                super.process(aCollateralReport);
                if (mOrder) {
                    mOrder = false;
                    OrderList ol = new OrderList();
                    int total = 10;
                    long seed = System.currentTimeMillis();
                    for (int i = 0; i < total; i++) {
                        OrderSingle orderSingle = MessageGenerator.generateMarketOrder(
                                aCollateralReport.getAccount(),
                                aCollateralReport.getQuantity(),
                                SideFactory.BUY,
                                "EUR/USD",
                                seed + "#order#" + i);
                        mOrderRequests.add(orderSingle.getSecondaryClOrdID());
                        ol.addOrder(orderSingle);
                    }
                    try {
                        mRequestId = mFxcmGateway.sendMessage(ol);
                        System.out.println("client: submitting order list with 10 new orders = " + mRequestId);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            public void process(PositionReport aPositionReport) {
                System.out.println("client inc: aPositionReport = " + aPositionReport);
                if (mOrderRequests.remove(aPositionReport.getSecondaryClOrdID())) {
                    if (mOrderRequests.isEmpty()) {
                        setSuccess(true);
                    }
                }
            }

            public void messageArrived(ITransportable aMessage) {
                handleMessage(aMessage, mAccounts, this);
            }
        }

        return doResult(new GenericListener());
    }

    public boolean testOTO() {
        String cem = Util.getCurrentlyExecutingMethod();
        System.out.println(cem);
        class GenericListener extends MessageTestHandler {
            private List mAccounts = new ArrayList();
            private boolean mOrder = true;
            private String mRequestId;
            private boolean mGotStop;
            private boolean mGotLimit;
            private boolean mGotPrimary;


            public void process(CollateralReport aCollateralReport) {
                super.process(aCollateralReport);
                if (mOrder) {
                    mOrder = false;
                    OrderList ol = new OrderList();
                    ol.setContingencyType(ContingencyTypeFactory.OTO);

                    OrderSingle orderSingle = MessageGenerator.generateStopLimitEntry(
                            1.2,
                            OrdTypeFactory.STOP,
                            aCollateralReport.getAccount(),
                            aCollateralReport.getQuantity(),
                            SideFactory.SELL,
                            "GBP/USD",
                            "primary order");
                    //OrderSingle orderSingle = MessageGenerator.generateMarketOrder(
                    //        aCollateralReport.getAccount(),
                    //        aCollateralReport.getQuantity(),
                    //        SideFactory.BUY,
                    //        "EUR/GBP",
                    //        "true market order test");
                    orderSingle.setClOrdLinkID(IFixDefs.CLORDLINKID_PRIMARY);
                    ol.addOrder(orderSingle);

                    OrderSingle stop = MessageGenerator.generateStopLimitEntry(
                            1.8,
                            OrdTypeFactory.STOP,
                            aCollateralReport.getAccount(),
                            aCollateralReport.getQuantity(),
                            SideFactory.BUY,
                            "GBP/USD",
                            "oto stop");
                    stop.setClOrdLinkID(IFixDefs.CLORDLINKID_CONTINGENT);
                    ol.addOrder(stop);

                    OrderSingle limit = MessageGenerator.generateStopLimitEntry(
                            1.1,
                            OrdTypeFactory.LIMIT,
                            aCollateralReport.getAccount(),
                            aCollateralReport.getQuantity(),
                            SideFactory.BUY,
                            "GBP/USD",
                            "oto limit");
                    limit.setClOrdLinkID(IFixDefs.CLORDLINKID_CONTINGENT);
                    ol.addOrder(limit);

                    try {
                        mRequestId = mFxcmGateway.sendMessage(ol);
                        System.out.println("client: good order requestid = " + mRequestId);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            public void process(ExecutionReport aExecutionReport) {
                super.process(aExecutionReport);
                if (aExecutionReport.getListID() != null && aExecutionReport.getListID().equalsIgnoreCase(mRequestId)) {
                    if ("primary order".equals(aExecutionReport.getSecondaryClOrdID())) {
                        mGotPrimary = true;
                    } else if ("oto stop".equalsIgnoreCase(aExecutionReport.getSecondaryClOrdID())) {
                        mGotStop = true;
                    } else if ("oto limit".equalsIgnoreCase(aExecutionReport.getSecondaryClOrdID())) {
                        mGotLimit = true;
                    }
                    if (mGotPrimary && mGotStop && mGotLimit) {
                        setSuccess(true);
                    }
                }
            }

            public void messageArrived(ITransportable aMessage) {
                handleMessage(aMessage, mAccounts, this);
            }
        }

        return doResult(new GenericListener());
    }

    public boolean testOTOCO() {
        String cem = Util.getCurrentlyExecutingMethod();
        System.out.println(cem);
        class GenericListener extends MessageTestHandler {
            private List mAccounts = new ArrayList();
            private boolean mOrder = true;
            private String mRequestId;
            private boolean mGotStop;
            private boolean mGotLimit;
            private boolean mGotPrimary;


            public void process(CollateralReport aCollateralReport) {
                super.process(aCollateralReport);
                if (mOrder) {
                    mOrder = false;
                    OrderList ol = new OrderList();
                    ol.setContingencyType(ContingencyTypeFactory.OTOCO);

                    //OrderSingle orderSingle = MessageGenerator.generateStopLimitEntry(
                    //        1.2,
                    //        OrdTypeFactory.STOP,
                    //        aCollateralReport.getAccount(),
                    //        aCollateralReport.getQuantity(),
                    //        SideFactory.SELL,
                    //        "GBP/USD",
                    //        "primary order");
                    OrderSingle orderSingle = MessageGenerator.generateMarketOrder(
                            aCollateralReport.getAccount(),
                            aCollateralReport.getQuantity(),
                            SideFactory.BUY,
                            "EUR/GBP",
                            "true market order test");
                    orderSingle.setClOrdLinkID(IFixDefs.CLORDLINKID_PRIMARY);
                    ol.addOrder(orderSingle);

                    OrderSingle stop = MessageGenerator.generateStopLimitEntry(
                            1.8,
                            OrdTypeFactory.STOP,
                            aCollateralReport.getAccount(),
                            aCollateralReport.getQuantity(),
                            SideFactory.BUY,
                            "GBP/USD",
                            "oto stop1");
                    stop.setClOrdLinkID(IFixDefs.CLORDLINKID_CONTINGENT);
                    ol.addOrder(stop);

                    OrderSingle stop2 = MessageGenerator.generateStopLimitEntry(
                            1.5,
                            OrdTypeFactory.STOP,
                            aCollateralReport.getAccount(),
                            aCollateralReport.getQuantity(),
                            SideFactory.BUY,
                            "EUR/USD",
                            "oto stop2");
                    stop2.setClOrdLinkID(IFixDefs.CLORDLINKID_CONTINGENT);
                    ol.addOrder(stop2);

                    try {
                        mRequestId = mFxcmGateway.sendMessage(ol);
                        System.out.println("client: good order requestid = " + mRequestId);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            public void process(ExecutionReport aExecutionReport) {
                super.process(aExecutionReport);
                if (aExecutionReport.getListID() != null && aExecutionReport.getListID().equalsIgnoreCase(mRequestId)) {
                    if ("primary order".equals(aExecutionReport.getSecondaryClOrdID())) {
                        mGotPrimary = true;
                    } else if ("oto stop".equalsIgnoreCase(aExecutionReport.getSecondaryClOrdID())) {
                        mGotStop = true;
                    } else if ("oto limit".equalsIgnoreCase(aExecutionReport.getSecondaryClOrdID())) {
                        mGotLimit = true;
                    }
                    if (mGotPrimary && mGotStop && mGotLimit) {
                        setSuccess(true);
                    }
                }
            }

            public void messageArrived(ITransportable aMessage) {
                handleMessage(aMessage, mAccounts, this);
            }
        }

        return doResult(new GenericListener());
    }

    public boolean testELS() {
        String cem = Util.getCurrentlyExecutingMethod();
        System.out.println(cem);
        class GenericListener extends MessageTestHandler {
            private List mAccounts = new ArrayList();
            private boolean mOrder = true;
            private String mRequestId;

            public void process(CollateralReport aCollateralReport) {
                super.process(aCollateralReport);
                if (mOrder) {
                    mOrder = false;
                    OrderList ol = new OrderList();
                    ol.setContingencyType(ContingencyTypeFactory.ELS);

                    //OrderSingle primary = MessageGenerator.generateMarketOrder(
                    //        aCollateralReport.getAccount(),
                    //        aCollateralReport.getQuantity(),
                    //        SideFactory.BUY,
                    //        "EUR/USD",
                    //        "primary order");
                    OrderSingle primary = MessageGenerator.generateStopLimitEntry(
                            1.5,
                            OrdTypeFactory.STOP,
                            aCollateralReport.getAccount(),
                            aCollateralReport.getQuantity(),
                            SideFactory.BUY,
                            "EUR/USD",
                            "primary order");

                    primary.setClOrdLinkID(IFixDefs.CLORDLINKID_PRIMARY);
                    ol.addOrder(primary);

                    OrderSingle stop = MessageGenerator.generateStopLimitEntry(
                            1.1,
                            OrdTypeFactory.STOP,
                            aCollateralReport.getAccount(),
                            aCollateralReport.getQuantity(),
                            SideFactory.SELL,
                            "EUR/USD",
                            "entry order - contingency - stop loss");
                    stop.setClOrdLinkID(IFixDefs.CLORDLINKID_CONTINGENT);
                    ol.addOrder(stop);

                    OrderSingle limit = MessageGenerator.generateStopLimitEntry(
                            1.6,
                            OrdTypeFactory.LIMIT,
                            aCollateralReport.getAccount(),
                            aCollateralReport.getQuantity(),
                            SideFactory.SELL,
                            "EUR/USD",
                            "entry order - contingency - limit profit");
                    limit.setClOrdLinkID(IFixDefs.CLORDLINKID_CONTINGENT);
                    ol.addOrder(limit);

                    try {
                        mRequestId = mFxcmGateway.sendMessage(ol);
                        System.out.println("client: good order requestid = " + mRequestId);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            public void process(PositionReport aPositionReport) {
                super.process(aPositionReport);
                if (safeEquals(aPositionReport.getListID(), mRequestId)) {
                    setSuccess(true);
                }
            }

            public void messageArrived(ITransportable aMessage) {
                handleMessage(aMessage, mAccounts, this);
            }
        }

        return doResult(new GenericListener());
    }

    public boolean testELSPeg() {
        String cem = Util.getCurrentlyExecutingMethod();
        System.out.println(cem);
        class GenericListener extends MessageTestHandler {
            private List mAccounts = new ArrayList();
            private boolean mOrder = true;
            private String mRequestId;

            public void process(CollateralReport aCollateralReport) {
                super.process(aCollateralReport);
                if (mOrder) {
                    mOrder = false;
                    OrderList ol = new OrderList();
                    ol.setContingencyType(ContingencyTypeFactory.ELS);

                    OrderSingle primary = MessageGenerator.generateMarketOrder(
                            aCollateralReport.getAccount(),
                            aCollateralReport.getQuantity(),
                            SideFactory.BUY,
                            "EUR/USD",
                            "primary order");
                    primary.setClOrdLinkID(IFixDefs.CLORDLINKID_PRIMARY);
                    ol.addOrder(primary);

                    OrderSingle stop = MessageGenerator.generateStopLimitEntry(
                            0,
                            OrdTypeFactory.STOP,
                            aCollateralReport.getAccount(),
                            aCollateralReport.getQuantity(),
                            SideFactory.SELL,
                            "EUR/USD",
                            "entry order - contingency - stop loss");
                    stop.setClOrdLinkID(IFixDefs.CLORDLINKID_CONTINGENT);
                    PegInstruction pegStop = new PegInstruction();
                    pegStop.setPegPriceType(IFixDefs.PEGPRICETYPE_MARKET);
                    pegStop.setPegMoveType(IFixDefs.PEGOFFSETTYPE_PRICE);
                    pegStop.setPegOffsetType(IFixDefs.PEGOFFSETTYPE_BASIS_POINTS);
                    pegStop.setPegOffsetValue(-10);
                    //pegStop.setFXCMPegFluctuatePts(10);//trailing stop
                    stop.setPegInstructions(pegStop);
                    ol.addOrder(stop);

                    OrderSingle limit = MessageGenerator.generateStopLimitEntry(
                            0,
                            OrdTypeFactory.LIMIT,
                            aCollateralReport.getAccount(),
                            aCollateralReport.getQuantity(),
                            SideFactory.SELL,
                            "EUR/USD",
                            "entry order - contingency - limit profit");
                    limit.setClOrdLinkID(IFixDefs.CLORDLINKID_CONTINGENT);
                    PegInstruction pegLimit = new PegInstruction();
                    pegLimit.setPegPriceType(IFixDefs.PEGPRICETYPE_MARKET);
                    pegLimit.setPegMoveType(IFixDefs.PEGOFFSETTYPE_PRICE);
                    pegLimit.setPegOffsetType(IFixDefs.PEGOFFSETTYPE_BASIS_POINTS);
                    pegLimit.setPegOffsetValue(10);
                    //pegLimit.setFXCMPegFluctuatePts(10);//trailing limit
                    limit.setPegInstructions(pegLimit);
                    ol.addOrder(limit);

                    try {
                        mRequestId = mFxcmGateway.sendMessage(ol);
                        System.out.println("client: good order requestid = " + mRequestId);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            public void process(PositionReport aPositionReport) {
                super.process(aPositionReport);
                if (safeEquals(aPositionReport.getListID(), mRequestId)) {
                    setSuccess(true);
                }
            }

            public void messageArrived(ITransportable aMessage) {
                handleMessage(aMessage, mAccounts, this);
            }
        }

        return doResult(new GenericListener());
    }

    public boolean testOrderStatusRequest() {
        final String cem = Util.getCurrentlyExecutingMethod();
        System.out.println(cem);
        class GenericListener extends MessageTestHandler {
            private List mAccounts = new ArrayList();
            private boolean mOrder = true;
            private boolean mRequestOrder = true;
            private String mRequestId;
            private String mOrdStatReqId;

            public void process(CollateralReport aCollateralReport) {
                super.process(aCollateralReport);
                try {
                    if (mOrder) {
                        mOrder = false;
                        OrderSingle orderSingle = MessageGenerator.generateMarketOrder(
                                aCollateralReport.getAccount(),
                                aCollateralReport.getQuantity(),
                                SideFactory.BUY,
                                "EUR/USD",
                                cem);
                        mRequestId = mFxcmGateway.sendMessage(orderSingle);
                        mOrdStatReqId = mFxcmGateway.requestOrderStatus(mRequestId,
                                                                        OrdStatusRequestType.CLORDID,
                                                                        aCollateralReport.getAccount());
                        System.out.println("client: market order requestId = " + mRequestId);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            public void process(ClosedPositionReport aClosedPositionReport) {
                if (safeEquals(mRequestId, aClosedPositionReport.getRequestID()) && mRequestOrder) {
                    System.out.println("aClosedPositionReport = " + aClosedPositionReport);
                    mRequestOrder = false;
                    mOrdStatReqId = mFxcmGateway.requestOrderStatus(aClosedPositionReport.getClOrdID(),
                                                                    OrdStatusRequestType.CLORDID,
                                                                    aClosedPositionReport.getAccount());
                    System.out.println("mOrdStatReqId = " + mOrdStatReqId);
                    System.out.println("\n");
                }
            }

            public void process(ExecutionReport aExe) {
                if (safeEquals(mRequestId, aExe.getRequestID())
                    && mRequestOrder
                    && aExe.getOrdStatus() == OrdStatusFactory.FILLED) {
                    System.out.println("\n\nclient: streaming aExecutionReport =\n" + aExe);
                    System.out.println("aExe.getInstrument() = " + aExe.getInstrument());
                    System.out.println("aExe.getTransactTime() = " + aExe.getTransactTime());
                    System.out.println("aExe.getExecType() = " + aExe.getExecType());
                    System.out.println("aExecutionReport.getOrderID() = " + aExe.getOrderID());
                    System.out.println("aExecutionReport.getClOrdID() = " + aExe.getClOrdID());
                    System.out.println("aExecutionReport.getOrdStatusReqID() = " + aExe.getOrdStatusReqID());
                    System.out.println("aExecutionReport.getSecondaryClOrdID() = " + aExe.getSecondaryClOrdID());
                    mRequestOrder = false;
                    mOrdStatReqId = mFxcmGateway.requestOrderStatus(aExe.getClOrdID(),
                                                                    OrdStatusRequestType.CLORDID,
                                                                    aExe.getAccount());
                    System.out.println("mOrdStatReqId = " + mOrdStatReqId);
                    try {
                        OrderSingle close = MessageGenerator.generateCloseMarketOrder(aExe.getFXCMPosID(),
                                                                                      aExe.getAccount(),
                                                                                      aExe.getCumQty(),
                                                                                      SideFactory.SELL,
                                                                                      aExe.getInstrument().getSymbol(),
                                                                                      "close it");
                        mFxcmGateway.sendMessage(close);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    System.out.println("\n");
                }
                if (safeEquals(mOrdStatReqId, aExe.getOrdStatusReqID())) {
                    System.out.println("\n\nclient: received order status request response =\n" + aExe);
                    System.out.println("aExe.getInstrument() = " + aExe.getInstrument());
                    System.out.println("aExe.getTransactTime() = " + aExe.getTransactTime());
                    System.out.println("aExe.getExecType() = " + aExe.getExecType());
                    System.out.println("aExecutionReport.getOrderID() = " + aExe.getOrderID());
                    System.out.println("aExecutionReport.getClOrdID() = " + aExe.getClOrdID());
                    System.out.println("aExecutionReport.getOrdStatusReqID() = " + aExe.getOrdStatusReqID());
                    System.out.println("aExecutionReport.getSecondaryClOrdID() = " + aExe.getSecondaryClOrdID());
                    System.out.println("\n");
                    setSuccess(true);
                } else {
                    System.out.println(aExe);
                }
            }

            public void messageArrived(ITransportable aMessage) {
                handleMessage(aMessage, mAccounts, this);
            }
        }

        return doResult(new GenericListener());
    }

    public boolean testPurgeAll() {
        final String cem = Util.getCurrentlyExecutingMethod();
        System.out.println(cem);
        class GenericListener extends MessageTestHandler {
            private List mAccounts = new ArrayList();
            private boolean mOrdersDone;
            private boolean mPositionsDone;

            public void process(RequestForPositionsAck aRequestForPositionsAck) {
                super.process(aRequestForPositionsAck);
                if (safeEquals(mOpenPositionMassID, aRequestForPositionsAck.getRequestID())) {
                    if (aRequestForPositionsAck.getTotalNumPosReports() == 0) {
                        mPositionsDone = true;
                    }
                    if (mPositionsDone && mOrdersDone) {
                        setSuccess(true);
                    }
                }
            }

            public void process(TradingSessionStatus aTradingSessionStatus) {
                mTradingSessionStatus = aTradingSessionStatus;
            }

            public void process(ExecutionReport aExecutionReport) {
                super.process(aExecutionReport);
                if (safeEquals(mOpenOrderMassID, aExecutionReport.getRequestID())) {
                    if (MessageAnalyzer.isStopLimitEntryOrder(aExecutionReport)) {
                        if (!aExecutionReport.getFXCMOrdStatus().isDeleted()) {
                            OrderCancelRequest ocr = new OrderCancelRequest();
                            System.out.println("client: deleting the entry order");
                            ocr.setOrderID(aExecutionReport.getOrderID());
                            ocr.setSecondaryClOrdID("text custom text delete order");
                            ocr.setOrigClOrdID(aExecutionReport.getClOrdID());
                            ocr.setSide(aExecutionReport.getSide());
                            ocr.setInstrument(aExecutionReport.getInstrument());
                            ocr.setAccount(aExecutionReport.getAccount());
                            ocr.setOrderQty(aExecutionReport.getOrderQty());
                            try {
                                String reqId = mFxcmGateway.sendMessage(ocr);
                                System.out.println("client: del reqId = " + reqId);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    if (aExecutionReport.isLastRptRequested()) {
                        if (aExecutionReport.getTotNumReports() > 0) {
                            mOpenOrderMassID = mFxcmGateway.requestOpenOrders(mUsername);
                        } else {
                            mOrdersDone = true;
                        }
                    }
                    if (mPositionsDone && mOrdersDone) {
                        setSuccess(true);
                    }
                }
            }

            public void process(PositionReport aPositionReport) {
                super.process(aPositionReport);
                if (safeEquals(mOpenPositionMassID, aPositionReport.getRequestID())) {
                    System.out.println("client: closing the open position");
                    ISide side;
                    // flip the side and set the correct price
                    if (aPositionReport.getPositionQty().getSide() == SideFactory.BUY) {
                        side = SideFactory.SELL;
                    } else {
                        side = SideFactory.BUY;
                    }
                    try {
                        String id = aPositionReport.getFXCMPosID();
                        boolean isFIFO = "N".equalsIgnoreCase(mTradingSessionStatus.getParameterValue("TP_94"))
                                         && "N".equalsIgnoreCase(mTradingSessionStatus.getParameterValue("TP_172"));
                        if (isFIFO) {
                            id = null;
                        }
                        OrderSingle delOrder = MessageGenerator.generateCloseMarketOrder(
                                id,
                                aPositionReport.getAccount(),
                                aPositionReport.getPositionQty().getQty(),
                                side,
                                aPositionReport.getInstrument().getSymbol(),
                                cem);
                        String mRequestId = mFxcmGateway.sendMessage(delOrder);
                        System.out.println("client: good order requestid = " + mRequestId);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (aPositionReport.isLastRptRequested() && aPositionReport.getTotalNumPosReports() > 0) {
                        new Thread(
                                new Runnable() {
                                    public void run() {
                                        try {
                                            Thread.sleep(10000);
                                            System.out.println("requesting open positions");
                                            mOpenPositionMassID = mFxcmGateway.requestOpenPositions(mUsername);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }).start();
                    }
                }
            }

            public void messageArrived(ITransportable aMessage) {
                handleMessage(aMessage, mAccounts, this);
            }
        }

        return doResult(new GenericListener());
    }

    public boolean testReconnect() {
        final String cem = Util.getCurrentlyExecutingMethod();
        System.out.println(cem);
        class GenericListener extends MessageTestHandler {
            private List mAccounts = new ArrayList();
            private boolean mDone;
            private boolean mOrder1 = true;
            private boolean mOrder2;
            private String mRequestId2;
            private String mRequestId1;

            public void process(PositionReport aPositionReport) {
                super.process(aPositionReport);
                if (safeEquals(aPositionReport.getClOrdID(), mRequestId1)) {
                    if (!mDone) {
                        System.out.println("attempting relogin");
                        try {
                            mFxcmGateway.relogin();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        System.out.println("done relogin");
                        mDone = true;
                        mOrder2 = true;
                    }
                }
                if (safeEquals(aPositionReport.getClOrdID(), mRequestId2)) {
                    setSuccess(true);
                }
            }

            public void process(MarketDataSnapshot aMarketDataSnapshot) {
                System.out.println(mFxcmGateway.getSessionID() + " " + aMarketDataSnapshot);
            }

            public void process(CollateralReport aCollateralReport) {
                try {
                    if (mOrder1) {
                        mOrder1 = false;
                        OrderSingle orderSingle = MessageGenerator.generateMarketOrder(
                                aCollateralReport.getAccount(),
                                aCollateralReport.getQuantity(),
                                SideFactory.BUY,
                                "EUR/USD",
                                cem);
                        mRequestId1 = mFxcmGateway.sendMessage(orderSingle);
                        System.out.println("client: order before relogin requestid = " + mRequestId1);
                    } else if (mOrder2) {
                        mOrder2 = false;
                        OrderSingle orderSingle = MessageGenerator.generateMarketOrder(
                                aCollateralReport.getAccount(),
                                aCollateralReport.getQuantity(),
                                SideFactory.BUY,
                                "EUR/USD",
                                cem);
                        mRequestId2 = mFxcmGateway.sendMessage(orderSingle);
                        System.out.println("client: order after relogin  requestid = " + mRequestId2);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            public void messageArrived(ITransportable aMessage) {
                handleMessage(aMessage, mAccounts, this);
            }
        }

        return doResult(new GenericListener());
    }

    public boolean testRejectDealerInterventionRequote() {
        final String cem = Util.getCurrentlyExecutingMethod();
        System.out.println(cem);
        class GenericListener extends MessageTestHandler {
            private List mAccounts = new ArrayList();
            private boolean mOrder = true;

            public void process(CollateralReport aCollateralReport) {
                try {
                    if (mOrder) {
                        mOrder = false;
                        OrderSingle orderSingle = MessageGenerator.generateMarketOrder(
                                aCollateralReport.getAccount(),
                                aCollateralReport.getQuantity(),
                                SideFactory.BUY,
                                "EUR/USD",
                                cem);
                        String requestId = mFxcmGateway.sendMessage(orderSingle);
                        System.out.println("client: order requestid = " + requestId);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            public void process(Quote aQuote) {
                super.process(aQuote);
                ITransportable msg = MessageGenerator.generatePassResponse(aQuote.getQuoteID());
                try {
                    mFxcmGateway.sendMessage(msg);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            public void process(ExecutionReport aExecutionReport) {
                super.process(aExecutionReport);
            }

            public void messageArrived(ITransportable aMessage) {
                handleMessage(aMessage, mAccounts, this);
            }
        }

        return doResult(new GenericListener());
    }

    public boolean testRejectMarketOrder() {
        final String cem = Util.getCurrentlyExecutingMethod();
        System.out.println(cem);
        class GenericListener extends MessageTestHandler {
            private List mAccounts = new ArrayList();
            private boolean mOrder = true;
            private String mRequestId;

            public void process(ExecutionReport aExecutionReport) {
                if (safeEquals(mRequestId, aExecutionReport.getClOrdID())) {
                    super.process(aExecutionReport);
                    if (aExecutionReport.getFXCMOrdStatus() == FXCMOrdStatusFactory.REJECTED) {
                        setSuccess(true);
                    }
                }
            }

            public void process(MarketDataSnapshot aMarketDataSnapshot) {
                super.process(aMarketDataSnapshot);
                try {
                    String symbol = aMarketDataSnapshot.getInstrument().getSymbol();
                    if (mOrder && !mAccounts.isEmpty() && symbol.indexOf("JPY") == -1) {
                        System.out.println("client: incoming mds = " + aMarketDataSnapshot);
                        mOrder = false;
                        CollateralReport acct = (CollateralReport) mAccounts.get(0);
                        OrderSingle orderSingle = MessageGenerator.generateOpenOrder(
                                aMarketDataSnapshot.getQuoteID() + 1, // purposefully bad quote id
                                aMarketDataSnapshot.getAskClose(),
                                acct.getAccount(),
                                acct.getQuantity(),
                                SideFactory.BUY,
                                aMarketDataSnapshot.getInstrument().getSymbol(),
                                cem);
                        mRequestId = mFxcmGateway.sendMessage(orderSingle);
                        System.out.println("client: bad order requestid = " + mRequestId);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            public void messageArrived(ITransportable aMessage) {
                handleMessage(aMessage, mAccounts, this);
            }
        }

        return doResult(new GenericListener());
    }

    public boolean testRFQ() {
        final String cem = Util.getCurrentlyExecutingMethod();
        System.out.println(cem);
        class GenericListener extends MessageTestHandler {
            private List mAccounts = new ArrayList();
            private boolean mFirst = true;
            private String mRequestId;

            public void process(TradingSessionStatus aTradingSessionStatus) {
                super.process(aTradingSessionStatus);
                mTradingSessionStatus = aTradingSessionStatus;
            }

            public void process(Quote aQuote) {
                super.process(aQuote);
                try {
                    //Using quote to submit an order
                    CollateralReport acct = (CollateralReport) getAccounts().get(0);
                    OrderSingle orderSingle = MessageGenerator.generateOpenOrder(
                            aQuote.getQuoteID(),
                            aQuote.getBidPx(),
                            acct.getAccount(),
                            aQuote.getOrderQty(),
                            SideFactory.BUY,
                            aQuote.getInstrument().getSymbol(),
                            cem);
                    mRequestId = mFxcmGateway.sendMessage(orderSingle);
                    System.out.println("client: good order requestid = " + mRequestId);

                    //delete the quote we are done with it
                    QuoteResponse qr = MessageGenerator.generatePassResponse(aQuote.getQuoteRespID());
                    String delQuoteID = mFxcmGateway.sendMessage(qr);
                    System.out.println("client: delete quote requestid = " + delQuoteID);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            public void process(PositionReport aPositionReport) {
                super.process(aPositionReport);
                if (safeEquals(aPositionReport.getClOrdID(), mRequestId)) {
                    setSuccess(true);
                }
            }

            public void process(MarketDataSnapshot aMarketDataSnapshot) {
                super.process(aMarketDataSnapshot);
                if (mFirst && !mAccounts.isEmpty()) {
                    try {
                        CollateralReport cr = (CollateralReport) getAccounts().get(0);
                        QuoteRequest qr = new QuoteRequest();
                        qr.setAccount(cr.getAccount());
                        qr.setOrderQty2(cr.getQuantity());
                        qr.setInstrument(mTradingSessionStatus.getSecurity("EUR/CHF"));
                        String reqid = mFxcmGateway.sendMessage(qr);
                        System.out.println("reqid = " + reqid);
                        mFirst = false;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            public void messageArrived(ITransportable aMessage) {
                handleMessage(aMessage, getAccounts(), this);
            }

            public List getAccounts() {
                return mAccounts;
            }
        }

        return doResult(new GenericListener());
    }

    public boolean testSecurityListRequest() {
        String cem = Util.getCurrentlyExecutingMethod();
        System.out.println(cem);
        class GenericListener extends MessageTestHandler {
            private String mReqID;

            public void process(SecurityList aSecurityList) {
                super.process(aSecurityList);
                if (safeEquals(mReqID, aSecurityList.getRequestID())) {
                    System.out.println("securityList = " + aSecurityList);
                    List securities = aSecurityList.getSecurities();
                    for (int i = 0; i < securities.size(); i++) {
                        Instrument instrument = (Instrument) securities.get(i);
                        try {
                            System.out.println(instrument.getSymbol() + " = " + instrument.getFXCMProductID());
                        } catch (NotDefinedException e) {
                            e.printStackTrace();
                        }
                    }
                    setSuccess(true);
                }
            }

            public void process(TradingSessionStatus aTradingSessionStatus) {
                super.process(aTradingSessionStatus);
                try {
                    SecurityListRequest securityListRequest =
                            new SecurityListRequest(SecurityListRequest.SECURITYLISTREQUESTTYPE_ALL);
                    mReqID = mFxcmGateway.sendMessage(securityListRequest);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            public void messageArrived(ITransportable aMessage) {
                handleMessage(aMessage, null, this);
            }
        }

        return doResult(new GenericListener());
    }

    public boolean testSecurityStatusRequest() {
        String cem = Util.getCurrentlyExecutingMethod();
        System.out.println(cem);
        class GenericListener extends MessageTestHandler {
            private String mReqID;

            public void process(SecurityStatus aSecurityStatus) {
                super.process(aSecurityStatus);
                if (safeEquals(mReqID, aSecurityStatus.getRequestID())) {
                    setSuccess(true);
                }
            }

            public void process(TradingSessionStatus aTradingSessionStatus) {
                super.process(aTradingSessionStatus);
                try {
                    SecurityStatusRequest ssr = new SecurityStatusRequest("GBP/JPY");
                    mReqID = mFxcmGateway.sendMessage(ssr);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            public void messageArrived(ITransportable aMessage) {
                handleMessage(aMessage, null, this);
            }
        }

        return doResult(new GenericListener());
    }

    public boolean testSetSLEntryOrder() {
        final String cem = Util.getCurrentlyExecutingMethod();
        System.out.println(cem);
        class GenericListener extends MessageTestHandler {
            private List mAccounts = new ArrayList();
            private boolean mOrder = true;
            private String mRequestId;
            private String mLimitReqID;
            private String mStopReqID;
            private boolean mGotLimitDelete;
            private boolean mGotStoptDelete;

            public void process(ExecutionReport aExe) {
                super.process(aExe);
                try {
                    String requestID = aExe.getRequestID();
                    if (MessageAnalyzer.isStopLimitEntryOrder(aExe) && safeEquals(mRequestId, requestID)) {
                        System.out.println("client: trying to set a limit on the entry order in 2 seconds");
                        OrderSingle limit = MessageGenerator.generateStopLimitClose(
                                aExe.getPrice() + .0080,
                                aExe.getFXCMPosID(),
                                OrdTypeFactory.LIMIT,
                                aExe.getAccount(),
                                aExe.getOrderQty(),
                                SideFactory.BUY,
                                aExe.getInstrument().getSymbol(),
                                cem);
                        mLimitReqID = mFxcmGateway.sendMessage(limit);
                        System.out.println("client: limit order reqId = " + mLimitReqID);

                        System.out.println("client: trying to set a stop on the entry order in 2 seconds");
                        OrderSingle stop = MessageGenerator.generateStopLimitClose(
                                aExe.getPrice() - .0080,
                                aExe.getFXCMPosID(),
                                OrdTypeFactory.STOP,
                                aExe.getAccount(),
                                aExe.getOrderQty(),
                                SideFactory.BUY,
                                aExe.getInstrument().getSymbol(),
                                cem);
                        mStopReqID = mFxcmGateway.sendMessage(stop);
                        System.out.println("client: stop  order reqId = " + mStopReqID);
                    } else if (safeEquals(mStopReqID, requestID) || safeEquals(mLimitReqID, requestID)) {
                        if (MessageAnalyzer.isLimitOrder(aExe)) {
                            mGotLimitDelete = true;
                            if (mGotStoptDelete) {
                                setSuccess(true);
                            }
                        } else {
                            mGotStoptDelete = true;
                            if (mGotLimitDelete) {
                                setSuccess(true);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            public void process(MarketDataSnapshot aMarketDataSnapshot) {
                super.process(aMarketDataSnapshot);
                try {
                    String symbol = aMarketDataSnapshot.getInstrument().getSymbol();
                    //choose the first non japanese mds for demonstration purposes
                    if (mOrder && !mAccounts.isEmpty() && symbol.indexOf("JPY") == -1) {
                        System.out.println("symbol = " + symbol);
                        mOrder = false;
                        CollateralReport acct = (CollateralReport) mAccounts.get(0);
                        OrderSingle orderSingle = MessageGenerator.generateStopLimitEntry(
                                aMarketDataSnapshot.getAskClose() + .0080,
                                OrdTypeFactory.STOP,
                                acct.getAccount(),
                                acct.getQuantity(),
                                SideFactory.BUY,
                                aMarketDataSnapshot.getInstrument().getSymbol(),
                                cem);
                        mRequestId = mFxcmGateway.sendMessage(orderSingle);
                        System.out.println("client: entry order requestId = " + mRequestId);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            public void messageArrived(ITransportable aMessage) {
                handleMessage(aMessage, mAccounts, this);
            }
        }

        return doResult(new GenericListener());
    }

    public boolean testSetSLTEntryOrder() {
        final String cem = Util.getCurrentlyExecutingMethod();
        System.out.println(cem);
        class GenericListener extends MessageTestHandler {
            private List mAccounts = new ArrayList();
            private boolean mOrder = true;
            private String mRequestId;
            private String mLimitReqID;
            private String mStopReqID;
            private boolean mGotLimitDelete;
            private boolean mGotStoptDelete;

            public void process(ExecutionReport aExe) {
                super.process(aExe);
                try {
                    String requestID = aExe.getRequestID();
                    if (MessageAnalyzer.isStopLimitEntryOrder(aExe) && safeEquals(mRequestId, requestID)) {
                        System.out.println("client: trying to set a limit on the entry order in 2 seconds");
                        OrderSingle limit = MessageGenerator.generateStopLimitClose(
                                0,
                                aExe.getFXCMPosID(),
                                OrdTypeFactory.LIMIT,
                                aExe.getAccount(),
                                aExe.getOrderQty(),
                                SideFactory.BUY,
                                aExe.getInstrument().getSymbol(),
                                cem);
                        PegInstruction pegLimit = new PegInstruction();
                        pegLimit.setPegPriceType(IFixDefs.PEGPRICETYPE_MARKET);
                        pegLimit.setPegMoveType(IFixDefs.PEGOFFSETTYPE_PRICE);
                        pegLimit.setPegOffsetType(IFixDefs.PEGOFFSETTYPE_BASIS_POINTS);
                        pegLimit.setPegOffsetValue(50);
                        pegLimit.setFXCMPegFluctuatePts(10); //trailing limit
                        limit.setPegInstructions(pegLimit);
                        mLimitReqID = mFxcmGateway.sendMessage(limit);
                        System.out.println("client: limit order reqId = " + mLimitReqID);

                        System.out.println("client: trying to set a stop on the entry order in 2 seconds");
                        OrderSingle stop = MessageGenerator.generateStopLimitClose(
                                0,
                                aExe.getFXCMPosID(),
                                OrdTypeFactory.STOP,
                                aExe.getAccount(),
                                aExe.getOrderQty(),
                                SideFactory.BUY,
                                aExe.getInstrument().getSymbol(),
                                cem);
                        PegInstruction pegStop = new PegInstruction();
                        pegStop.setPegPriceType(IFixDefs.PEGPRICETYPE_MARKET);
                        pegStop.setPegMoveType(IFixDefs.PEGOFFSETTYPE_PRICE);
                        pegStop.setPegOffsetType(IFixDefs.PEGOFFSETTYPE_BASIS_POINTS);
                        pegStop.setPegOffsetValue(-50);
                        pegStop.setFXCMPegFluctuatePts(10); //trailing stop
                        stop.setPegInstructions(pegStop);
                        mStopReqID = mFxcmGateway.sendMessage(stop);
                        System.out.println("client: stop  order reqId = " + mStopReqID);
                    } else if (safeEquals(mStopReqID, requestID) || safeEquals(mLimitReqID, requestID)) {
                        if (MessageAnalyzer.isLimitOrder(aExe)) {
                            mGotLimitDelete = true;
                            if (mGotStoptDelete) {
                                setSuccess(true);
                            }
                        } else {
                            mGotStoptDelete = true;
                            if (mGotLimitDelete) {
                                setSuccess(true);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            public void process(MarketDataSnapshot aMarketDataSnapshot) {
                super.process(aMarketDataSnapshot);
                try {
                    String symbol = aMarketDataSnapshot.getInstrument().getSymbol();
                    //choose the first non japanese mds for demonstration purposes
                    if (mOrder && !mAccounts.isEmpty() && symbol.indexOf("JPY") == -1) {
                        System.out.println("symbol = " + symbol);
                        mOrder = false;
                        CollateralReport acct = (CollateralReport) mAccounts.get(0);
                        OrderSingle orderSingle = MessageGenerator.generateStopLimitEntry(
                                aMarketDataSnapshot.getAskClose() + .0080,
                                OrdTypeFactory.STOP,
                                acct.getAccount(),
                                acct.getQuantity(),
                                SideFactory.BUY,
                                aMarketDataSnapshot.getInstrument().getSymbol(),
                                cem,
                                10);
                        mRequestId = mFxcmGateway.sendMessage(orderSingle);
                        System.out.println("client: entry order requestId = " + mRequestId);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            public void messageArrived(ITransportable aMessage) {
                handleMessage(aMessage, mAccounts, this);
            }
        }

        return doResult(new GenericListener());
    }

    public boolean testSetSLMarketOrder() {
        final String cem = Util.getCurrentlyExecutingMethod();
        System.out.println(cem);
        class GenericListener extends MessageTestHandler {
            private List mAccounts = new ArrayList();
            private boolean mOrder = true;
            private boolean mFlag = true;
            private String mRequestId;
            private String mLimitReqID;
            private String mStopReqID;
            private boolean mGotLimitDelete;
            private boolean mGotStoptDelete;

            public void process(PositionReport aPositionReport) {
                super.process(aPositionReport);
                if (mFlag && safeEquals(mRequestId, aPositionReport.getRequestID())) {
                    // only do it the first time for testing purposes
                    try {
                        System.out.println("client: trying to set a stop on the position in 2 seconds");
                        OrderSingle order = MessageGenerator.generateStopLimitClose(
                                aPositionReport.getSettlPrice() - .0010,
                                aPositionReport.getFXCMPosID(),
                                OrdTypeFactory.STOP,
                                aPositionReport.getAccount(),
                                aPositionReport.getPositionQty().getQty(),
                                SideFactory.BUY,
                                aPositionReport.getInstrument().getSymbol(),
                                cem);
                        PegInstruction pegStop = new PegInstruction();
                        pegStop.setFXCMPegFluctuatePts(1);
                        order.setPegInstructions(pegStop);
                        mStopReqID = mFxcmGateway.sendMessage(order);
                        System.out.println("client: stop order reqId = " + mStopReqID);

                        System.out.println("client: trying to set a limit on the position in 2 seconds");
                        order = MessageGenerator.generateStopLimitClose(
                                aPositionReport.getSettlPrice() + .0010,
                                aPositionReport.getFXCMPosID(),
                                OrdTypeFactory.LIMIT,
                                aPositionReport.getAccount(),
                                aPositionReport.getPositionQty().getQty(),
                                SideFactory.BUY,
                                aPositionReport.getInstrument().getSymbol(),
                                cem);
                        mLimitReqID = mFxcmGateway.sendMessage(order);
                        System.out.println("client: limit order reqId = " + mLimitReqID);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    mFlag = false;
                }
            }

            public void process(CollateralReport aCollateralReport) {
                try {
                    if (mOrder) {
                        mOrder = false;
                        OrderSingle orderSingle = MessageGenerator.generateMarketOrder(
                                aCollateralReport.getAccount(),
                                aCollateralReport.getQuantity(),
                                SideFactory.BUY,
                                "EUR/USD",
                                null);
                        mRequestId = mFxcmGateway.sendMessage(orderSingle);
                        System.out.println("client: good order requestid = " + mRequestId);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            public void process(ExecutionReport aExecutionReport) {
                super.process(aExecutionReport);
                if (safeEquals(mStopReqID, aExecutionReport.getRequestID()) || safeEquals(
                        mLimitReqID, aExecutionReport.getRequestID())) {
                    if (MessageAnalyzer.isLimitOrder(aExecutionReport)) {
                        mGotLimitDelete = true;
                        if (mGotStoptDelete) {
                            setSuccess(true);
                        }
                    } else {
                        mGotStoptDelete = true;
                        if (mGotLimitDelete) {
                            setSuccess(true);
                        }
                    }
                }
            }

            public void messageArrived(ITransportable aMessage) {
                handleMessage(aMessage, mAccounts, this);
            }
        }

        return doResult(new GenericListener());
    }

    public boolean testUpdateRateQtyEntryOrder() {
        final String cem = Util.getCurrentlyExecutingMethod();
        System.out.println(cem);
        class GenericListener extends MessageTestHandler {
            private List mAccounts = new ArrayList();
            private boolean mOrder = true;
            private boolean mFlag = true;
            private String mRequestId;

            public void process(CollateralReport aCollateralReport) {
                super.process(aCollateralReport);
                OrderSingle os = MessageGenerator.generateStopLimitEntry(
                        1.1,
                        OrdTypeFactory.STOP,
                        aCollateralReport.getAccount(),
                        aCollateralReport.getQuantity(),
                        SideFactory.SELL,
                        "EUR/USD",
                        cem);
                try {
                    mRequestId = mFxcmGateway.sendMessage(os);
                    System.out.println("client: entry order requestId = " + mRequestId);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            public void process(ExecutionReport aExe) {
                super.process(aExe);
                try {
                    if (MessageAnalyzer.isStopLimitEntryOrder(aExe) && safeEquals(mRequestId, aExe.getRequestID())) {
                        if (mFlag) {
                            mFlag = false;
                            System.out.println("client: updating the entry price");
                            OrderCancelReplaceRequest os =
                                    MessageGenerator.generateOrderReplaceRequest(
                                            "test custom text entry",
                                            aExe.getOrderID(),
                                            aExe.getSide(),
                                            aExe.getOrdType(),
                                            1.2,
                                            aExe.getAccount());
                            os.setOrderID(aExe.getOrderID());
                            os.setOrigClOrdID(aExe.getClOrdID());
                            os.setSide(aExe.getSide());
                            os.setInstrument(aExe.getInstrument());
                            os.setLeavesQty(aExe.getOrderQty() * 5);
                            mRequestId = mFxcmGateway.sendMessage(os);
                        } else {
                            setSuccess(true);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            public void messageArrived(ITransportable aMessage) {
                handleMessage(aMessage, mAccounts, this);
            }
        }

        return doResult(new GenericListener());
    }

    public boolean testUpdateSLEntryOrder() {
        final String cem = Util.getCurrentlyExecutingMethod();
        System.out.println(cem);
        class GenericListener extends MessageTestHandler {
            private List mAccounts = new ArrayList();
            private boolean mOrder = true;
            private boolean mFlag1 = true;
            private boolean mFlag2 = true;
            private String mRequestId;
            private String mLimitId;
            private String mStopId;
            private boolean mGotLimitDelete;
            private boolean mGotStoptDelete;

            public void process(MarketDataSnapshot aMarketDataSnapshot) {
                super.process(aMarketDataSnapshot);
                try {
                    String symbol = aMarketDataSnapshot.getInstrument().getSymbol();
                    //choose the first non japanese mds for demonstration purposes
                    if (mOrder && !mAccounts.isEmpty() && symbol.indexOf("JPY") == -1) {
                        CollateralReport acct = (CollateralReport) mAccounts.get(0);
                        mOrder = false;
                        OrderSingle orderSingle = MessageGenerator.generateStopLimitEntry(
                                aMarketDataSnapshot.getAskClose() + .0080,
                                OrdTypeFactory.STOP,
                                acct.getAccount(),
                                acct.getQuantity(),
                                SideFactory.BUY,
                                aMarketDataSnapshot.getInstrument().getSymbol(),
                                cem
                        );
                        mRequestId = mFxcmGateway.sendMessage(orderSingle);
                        System.out.println("client: entry order requestId = " + mRequestId);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            public void process(ExecutionReport aExe) {
                super.process(aExe);
                try {
                    if (MessageAnalyzer.isStopLimitEntryOrder(aExe) && safeEquals(mRequestId, aExe.getRequestID())) {
                        System.out.println("client: trying to set a limit on the entry order");
                        OrderSingle order = MessageGenerator.generateStopLimitClose(
                                aExe.getPrice() + .0080,
                                aExe.getFXCMPosID(),
                                OrdTypeFactory.LIMIT,
                                aExe.getAccount(),
                                aExe.getOrderQty(),
                                SideFactory.BUY,
                                aExe.getInstrument().getSymbol(),
                                cem);
                        mLimitId = mFxcmGateway.sendMessage(order);
                        System.out.println("client: limit order reqId = " + mLimitId);

                        System.out.println("client: trying to set a stop on the entry order");
                        order = MessageGenerator.generateStopLimitClose(
                                aExe.getPrice() - .0080,
                                aExe.getFXCMPosID(),
                                OrdTypeFactory.STOP,
                                aExe.getAccount(),
                                aExe.getOrderQty(),
                                SideFactory.BUY,
                                aExe.getInstrument().getSymbol(),
                                cem);
                        mStopId = mFxcmGateway.sendMessage(order);
                        System.out.println("client: stop  order reqId = " + mStopId);
                    } else if (MessageAnalyzer.isStopLimitCloseOrder(aExe)) {
                        if (MessageAnalyzer.isLimitOrder(aExe) && safeEquals(mLimitId, aExe.getRequestID())) {
                            if (mFlag1) {
                                System.out.println("client: updating a limit order");
                                OrderCancelReplaceRequest os = MessageGenerator.generateOrderReplaceRequest(
                                        "test custom text limit order",
                                        aExe.getOrderID(),
                                        aExe.getSide(),
                                        aExe.getOrdType(),
                                        aExe.getPrice() + .0040,
                                        aExe.getAccount());
                                os.setOrderID(aExe.getOrderID());
                                os.setOrigClOrdID(aExe.getClOrdID());
                                os.setSide(aExe.getSide());
                                os.setInstrument(aExe.getInstrument());
                                os.setOrderQty(aExe.getOrderQty());
                                mLimitId = mFxcmGateway.sendMessage(os);
                                mFlag1 = false;
                            } else {
                                mGotLimitDelete = true;
                                if (mGotStoptDelete) {
                                    setSuccess(true);
                                }
                            }
                        } else if (MessageAnalyzer.isStopOrder(aExe) && safeEquals(mStopId, aExe.getRequestID())) {
                            if (mFlag2) {
                                System.out.println("client: updating a stop order");
                                OrderCancelReplaceRequest os = MessageGenerator.generateOrderReplaceRequest(
                                        "test custom text stop order",
                                        aExe.getOrderID(),
                                        aExe.getSide(),
                                        aExe.getOrdType(),
                                        aExe.getPrice() - .0040,
                                        aExe.getAccount());
                                os.setOrderID(aExe.getOrderID());
                                os.setOrigClOrdID(aExe.getClOrdID());
                                os.setSide(aExe.getSide());
                                os.setInstrument(aExe.getInstrument());
                                os.setOrderQty(aExe.getOrderQty());
                                mStopId = mFxcmGateway.sendMessage(os);
                                mFlag2 = false;
                            } else {
                                mGotStoptDelete = true;
                                if (mGotLimitDelete) {
                                    setSuccess(true);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            public void messageArrived(ITransportable aMessage) {
                handleMessage(aMessage, mAccounts, this);
            }
        }

        return doResult(new GenericListener());
    }

    public boolean testUpdateSLMarketOrder() {
        final String cem = Util.getCurrentlyExecutingMethod();
        System.out.println(cem);
        class GenericListener extends MessageTestHandler {
            private List mAccounts = new ArrayList();
            private boolean mOrder = true;
            private boolean mFlag1 = true;
            private boolean mFlag2 = true;
            private String mRequestId;
            private String mStopId;
            private String mLimitId;
            private boolean mGotLimitDelete;
            private boolean mGotStoptDelete;

            public void process(ExecutionReport aExecutionReport) {
                super.process(aExecutionReport);
                if (MessageAnalyzer.isStopLimitCloseOrder(aExecutionReport)) {
                    try {
                        if (MessageAnalyzer.isLimitOrder(aExecutionReport)
                            && safeEquals(mLimitId, aExecutionReport.getRequestID())) {
                            if (mFlag1) {
                                mFlag1 = false;
                                System.out.println("\nupdating a limit order in 5 sec");
                                OrderCancelReplaceRequest os = MessageGenerator.generateOrderReplaceRequest(
                                        "test custom text limit",
                                        aExecutionReport.getOrderID(),
                                        aExecutionReport.getSide(),
                                        aExecutionReport.getOrdType(),
                                        aExecutionReport.getPrice() + .0040,
                                        aExecutionReport.getAccount());
                                os.setOrderID(aExecutionReport.getOrderID());
                                os.setOrigClOrdID(aExecutionReport.getClOrdID());
                                os.setSide(aExecutionReport.getSide());
                                os.setInstrument(aExecutionReport.getInstrument());
                                os.setOrderQty(aExecutionReport.getOrderQty());
                                mLimitId = mFxcmGateway.sendMessage(os);
                            } else {
                                mGotLimitDelete = true;
                                if (mGotStoptDelete) {
                                    setSuccess(true);
                                }
                            }
                        } else if (MessageAnalyzer.isStopOrder(aExecutionReport) && safeEquals(
                                mStopId, aExecutionReport.getRequestID())) {
                            if (mFlag2) {
                                mFlag2 = false;
                                System.out.println("\nupdating a stop order in 5 sec");
                                OrderCancelReplaceRequest os =
                                        MessageGenerator.generateOrderReplaceRequest(
                                                "test custom text stop",
                                                aExecutionReport.getOrderID(),
                                                aExecutionReport.getSide(),
                                                aExecutionReport.getOrdType(),
                                                aExecutionReport.getPrice() - .0040,
                                                aExecutionReport.getAccount());
                                os.setOrderID(aExecutionReport.getOrderID());
                                os.setOrigClOrdID(aExecutionReport.getClOrdID());
                                os.setSide(aExecutionReport.getSide());
                                os.setInstrument(aExecutionReport.getInstrument());
                                os.setOrderQty(aExecutionReport.getOrderQty());
                                mStopId = mFxcmGateway.sendMessage(os);
                            } else {
                                mGotStoptDelete = true;
                                if (mGotLimitDelete) {
                                    setSuccess(true);
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            public void process(CollateralReport aCollateralReport) {
                try {
                    if (mOrder) {
                        OrderSingle orderSingle = MessageGenerator.generateMarketOrder(
                                aCollateralReport.getAccount(),
                                aCollateralReport.getQuantity(),
                                SideFactory.BUY,
                                "EUR/USD",
                                cem);
                        mRequestId = mFxcmGateway.sendMessage(orderSingle);
                        System.out.println("client: good order requestid = " + mRequestId);
                        mOrder = false;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            public void process(PositionReport aPositionReport) {
                super.process(aPositionReport);
                if (safeEquals(mRequestId, aPositionReport.getRequestID())) {
                    System.out.println("client: inc pos report = " + aPositionReport);
                    try {
                        System.out.println("\nclient: trying to set a stop on the open position");
                        OrderSingle order = MessageGenerator.generateStopLimitClose(
                                aPositionReport.getSettlPrice() - .0080,
                                aPositionReport.getFXCMPosID(),
                                OrdTypeFactory.STOP,
                                aPositionReport.getAccount(),
                                aPositionReport.getPositionQty().getQty(),
                                SideFactory.BUY,
                                aPositionReport.getInstrument().getSymbol(),
                                cem);
                        mStopId = mFxcmGateway.sendMessage(order);
                        System.out.println("client: stop  order reqId = " + mStopId);

                        System.out.println("\nclient: trying to set a limit on the open position");
                        order = MessageGenerator.generateStopLimitClose(
                                aPositionReport.getSettlPrice() + .0080,
                                aPositionReport.getFXCMPosID(),
                                OrdTypeFactory.LIMIT,
                                aPositionReport.getAccount(),
                                aPositionReport.getPositionQty().getQty(),
                                SideFactory.BUY,
                                aPositionReport.getInstrument().getSymbol(),
                                cem);
                        mLimitId = mFxcmGateway.sendMessage(order);
                        System.out.println("client: limit order reqId = " + mLimitId);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            public void messageArrived(ITransportable aMessage) {
                handleMessage(aMessage, mAccounts, this);
            }
        }

        return doResult(new GenericListener());
    }

    public boolean testUserStatus() {
        String cem = Util.getCurrentlyExecutingMethod();
        System.out.println(cem);
        class GenericListener extends MessageTestHandler {
            private String mRID;

            public void process(UserResponse aUserResponse) {
                System.out.println("\naUserResponse = " + aUserResponse);
                if (aUserResponse.getRequestID().equals(mRID)) {
                    setSuccess(true);
                }
            }

            public void process(TradingSessionStatus aTradingSessionStatus) {
                super.process(aTradingSessionStatus);
                UserRequest req = new UserRequest();
                req.setUsername(mUsername);
                req.setPassword(mPassword);
                req.setUserRequestType(IFixDefs.USERREQUESTTYPE_USERSTATUS);
                try {
                    mRID = mFxcmGateway.sendMessage(req);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            public void process(Logout aLogout) {
                super.process(aLogout);
                UserRequest req = new UserRequest();
                req.setUsername(mUsername);
                req.setPassword(mPassword);
                req.setUserRequestType(IFixDefs.USERREQUESTTYPE_USERSTATUS);
                try {
                    mRID = mFxcmGateway.sendMessage(req);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            public void messageArrived(ITransportable aMessage) {
                handleMessage(aMessage, null, this);
            }
        }

        return doResult(new GenericListener());
    }

    private static class DefaultStatusListener implements IStatusMessageListener {
        private boolean mPrint;

        DefaultStatusListener(boolean aPrint) {
            mPrint = aPrint;
        }

        public void messageArrived(ISessionStatus aStatus) {
            if (mPrint) {
                System.out.println("aStatus = " + aStatus);
            }
            if (aStatus.getStatusCode() == ISessionStatus.STATUSCODE_ERROR
                || aStatus.getStatusCode() == ISessionStatus.STATUSCODE_DISCONNECTING
                || aStatus.getStatusCode() == ISessionStatus.STATUSCODE_DISCONNECTED) {
                System.out.println("aStatus = " + aStatus);
            }
        }
    }

    private abstract class MessageTestHandler implements IGenericMessageListener {
        private boolean mSuccess;
        protected TradingSessionStatus mTradingSessionStatus;

        public boolean isSuccess() {
            return mSuccess;
        }

        public void setSuccess(boolean aSuccess) {
            mSuccess = aSuccess;
        }

        public void process(UserResponse aUserResponse) {
            System.out.println("client inc: aUserResponse = " + aUserResponse);
        }

        public void process(CollateralInquiryAck aCollateralInquiryAck) {
            System.out.println("client inc: aCollateralInquiryAck = " + aCollateralInquiryAck);
        }

        public void process(CollateralReport aCollateralReport) {
            System.out.println("client inc: aCollateralReport = " + aCollateralReport);
            if (aCollateralReport.isLastRptRequested()) {
                try {
                    System.out.println("client out: do marketdatarequest for testing to get fast mds");
                    MarketDataRequest mdr = new MarketDataRequest();
                    Enumeration securities = mTradingSessionStatus.getSecurities();
                    while (securities.hasMoreElements()) {
                        TradingSecurity o = (TradingSecurity) securities.nextElement();
                        mdr.addRelatedSymbol(o);
                    }
                    mdr.setSubscriptionRequestType(SubscriptionRequestTypeFactory.SUBSCRIBE);
                    mdr.setMDEntryTypeSet(MarketDataRequest.MDENTRYTYPESET_ALL);
                    mFxcmGateway.sendMessage(mdr);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        public void process(ExecutionReport aExecutionReport) {
            System.out.println("client inc: aExecutionReport = " + aExecutionReport);
        }

        public void process(ClosedPositionReport aClosedPositionReport) {
            System.out.println("client inc: aClosedPositionReport = " + aClosedPositionReport);
        }

        public void process(PositionReport aPositionReport) {
            System.out.println("client inc: aPositionReport = " + aPositionReport);
        }

        public void process(BusinessMessageReject aBusinessMessageReject) {
            System.out.println("client inc: aBusinessMessageReject = " + aBusinessMessageReject);
        }

        public void process(RequestForPositionsAck aRequestForPositionsAck) {
            System.out.println("client inc: aRequestForPositionsAck = " + aRequestForPositionsAck);
        }

        public void process(TradingSessionStatus aTradingSessionStatus) {
            mTradingSessionStatus = aTradingSessionStatus;
            System.out.println("client inc: aTradingSessionStatus = " + aTradingSessionStatus);
        }

        public void process(SecurityStatus aSecurityStatus) {
            System.out.println("client inc: aSecurityStatus = " + aSecurityStatus);
        }

        public void process(SecurityList aSecurityList) {
            System.out.println("client inc: aSecurityList = " + aSecurityList);
        }

        public void process(Quote aQuote) {
            System.out.println("client inc: aQuote = " + aQuote);
        }

        public void process(MarketDataSnapshot aMarketDataSnapshot) {
            //nothing
        }

        public void process(MarketDataRequestReject aMarketDataRequestReject) {
        }

        public void process(Logout aLogout) {
            System.out.println("client inc: aLogout = " + aLogout);
        }
    }

    public static void main(String[] aArgs) {
        if (aArgs.length < 4) {
            System.out.println("must supply 4 arguments: username, password, station, hostname, command, config");
            return;
        }
        if (aArgs.length > 4) {
            runTest(aArgs);
        }
    }
}
