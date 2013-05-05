/*
 * $Id: ApiUsage.java 332 2009-03-17 15:54:39Z ykudryashov $
 */
package gft.api.example;

import technoor.service.gftuk.InstrumentIds;
import gft.api.*;
import gft.api.profile.ContractSizeProfile;
import gft.api.profile.MRPProfile;
import technoor.service.IServiceUpdater;
import gft.api.example.ServiceUpdate;
import org.apache.log4j.PropertyConfigurator;
import java.rmi.RemoteException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.io.*;
import technoor.service.IPricingService;
import technoor.service.gftuk.GFTUKLogin;

/**
 * This class demonstrates the usage of GFT Public API.
 *
 * @author Boris Malkin
 */
public class ApiUsage {

    public static final String CLSID = "$Id: ApiUsage.java 332 2009-03-17 15:54:39Z ykudryashov $";
    public final static String URL_PROP_NAME = "gft.url";
    public final static String LOGIN_PROP_NAME = "gft.login";
    public final static String PASSWORD_PROP_NAME = "gft.password";
    private final static long _40_MIN = 40 * 60 * 1000;
    private final static long _DAY = 24 * 60 * 60 * 1000;
    private IServiceUpdater sHandleRequests;

    ApiUsage() {
        try {
            sHandleRequests = new gft.api.example.ServiceUpdate();
        } catch (Exception e) {
            log("exception");
            e.printStackTrace();
        }
    }
    private final MarketState marketState = new MarketState();
    /**
     * SessionStateListener implementation
     */
    private SessionStateListener ssListener = new SessionStateAdapter();
    /**
     * AccountUpdateListener implementation
     */
    private AccountUpdateListener auListener = new AccountUpdateAdapter();
    /**
     * OrderUpdateListener implementation
     */
    private OrderUpdateListener ouListener = new OrderUpdateAdapter();
    /**
     * MessageUpdateListener implementation
     */
    private MessageUpdateListener muListener = new MessageUpdateAdapter() {
        public void onNewsUpdate(Message message) {
            super.onNewsUpdate(message);
            log(message + ": URL = '" + message.getBody() + "'");
        }
    };
    /**
     * creating QuoteUpdateListener implementation
     */
    private QuoteUpdateListener quListener = new QuoteUpdateAdapter() {
        public void onQuotesUpdate(Quote[] quotes) {
            super.onQuotesUpdate(quotes);
            marketState.updateMarketState(quotes);
        }
    };

    private Pricing initSession(Session session, String connectionString, String login, String password) throws RemoteException {

        // obtain business api-s

        final UserSupport userSupport = session.getUserSupport();
        final Pricing pricing = session.getPricing();
        final Trading trading = session.getTrading();
        final Messaging messaging = session.getMessaging();

        // register listeners to process server callbacks

        session.addSessionStateListener(ssListener);
        userSupport.addAccountUpdateListener(auListener);
        pricing.addQuoteUpdateListener(quListener);
        trading.addOrderUpdateListener(ouListener);
        messaging.addMessageUpdateListener(muListener);

        // logging in

        try {
            final LoginContext context = session.start(
                    new LoginRequest(login, password,
                    TradingPermission.SB.getFlag() | TradingPermission.FX.getFlag() | TradingPermission.CFD.getFlag(),
                    LoginRequest.MultipleLoginType.SINGLE_FORCE_LOGOUT));
            log("logged in; login context = " + context);
            log();

            AccountDetails3[] accountDetails = callListAccountsDetails3(userSupport);

            // extracting detailed information from the AccountDetails object

            if (accountDetails.length > 0) {

                AccountDetails3 account = accountDetails[0];

                log("    FloatingPL         : " + account.getFloatingPL(marketState));
                log("    TicketFloatingPL   : " + account.getTicketFloatingPL(marketState));
                log("    Cash               : " + account.getCash());
                log("    UnrealizedPL       : " + account.getUnrealizedPL());
                log("    Forex Margin Req   : " + account.getForexMarginRequirement(marketState));
                log("    CFD/SB Margin Req  : " + account.getTicketMarginRequirement(marketState));
                log("    Total Margin Req   : " + account.getMarginRequirement(marketState));

                log("MRPs per instrument:");
                MRPProfile mrp = account.getMRPProfile();
                /*                for (int i = 0; i < instruments.length; i++) {
                 log("    " + instruments[i] + " : " + mrp.getMRP(instruments[i].getInstrumentId()));
                 }

                 logArray("Positions:", account.getPositions());
                 log();
                 logArray("CashBalances:", account.getCashBalances());
                 log();
                 logArray("Tickets:", account.getTickets());
                 log();

                 // listing incomplete orders

                 callListIncompleteOrders(trading, account);

                 // listing orders history

                 callListOrdersHistory(userSupport, account);

                 // listing transfers history

                 callListTransfersHistory(userSupport, account);

                 // perform a series of order-related operations

                 // callOrderOperations(trading, userSupport, account);
                 */
            }
        } catch (gft.api.VersionException ve) {
            ve.printStackTrace();
            return null;
        } catch (gft.api.LoginException le) {
            le.printStackTrace();
            return null;
        }

        return pricing;
    }

    private Session getSession(String connectionString) {
        try {
            return SessionFactory.getSession(connectionString);
        } catch (SessionInstantiationException sie) {
            sie.printStackTrace();
        }
        return null;
    }

    private void go(IPricingService.LoginProperty l) {
        String connectionString = l.getUrl();
        String login = l.getLogin();
        String password = l.getPwd();

        Session session = null;

        try {
            session = getSession(connectionString);



            // creating session object

            final Pricing pricing = initSession(session, connectionString, login, password);;


// Main Program Loop
            // listing candles history
            int wfu;
            try {
                Instrument[] availInstruments = callListAvailableInstruments(session.getUserSupport());

                while ((wfu = sHandleRequests.waitForConnection()) > 0) {
                    System.out.println("Getting data " + sHandleRequests.getInstrumentId());
                    if (!isInstrumentAvailable(sHandleRequests.getInstrumentId())) {
                        continue;
                    }
                    Thread.sleep(1000);
                    callListCandleHistory(pricing, wfu, sHandleRequests.getInstrumentId(), sHandleRequests.getNumOfDays());
                    
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            // listing quotes snapshot

            // callListQuotes(pricing);

            // listing news

            //callListNews(messaging);

            // listing available instruments

            //Instrument[] availInstruments = callListAvailableInstruments(userSupport);

            // listing instruments, selected by the user

            //Instrument2[] instruments = callListSelectedInstruments(userSupport);

            // listing brief information about user accounts

            // callListAccounts(userSupport);

            // change the list of selected instruments

            //  callChangeSelectedInstruments(userSupport);

            // listing detailed information about user accounts




        } catch (RemoteException e) {
            log("Critical Error Exception");
        } finally {
            if (session != null) {
                try {
                    session.close(); // finishing session
                } catch (RemoteException e) {
                    log("Critical Error Exception");
                }
            }
        }
    }

    private void callListLastTicks(Pricing pricing) throws RemoteException {
        int[] instrument_ids = new int[]{
            InstrumentIds.AUD_USD,
            InstrumentIds.GBP_AUD
        };
        int max_count = 50;
        Hashtable qhistory = pricing.listLastTicks(instrument_ids, max_count);
        logHashtable("pricing.listLastTicks():", qhistory);
        log();
        for (Enumeration e = qhistory.keys(); e.hasMoreElements();) {
            Object key = e.nextElement();
            Object value = qhistory.get(key);
            gft.api.Quote[] quotes = (gft.api.Quote[]) value;
            for (int i = 0; i < quotes.length; i++) {
                log("    instrument=" + key + ", quote=" + quotes[i]);
            }
        }
    }

    // private Candle[] callListCandleHistory(Pricing pricing, int wfc) throws RemoteException{
    // 	return	 callListCandleHistory(pricing, wfc, 3, 5);
    // }
    private Candle[] callListCandleHistory(Pricing pricing, int wfc, int insId, int numDays) throws RemoteException {
        int max_count = wfc;
        System.out.println("Count: " + wfc + " NumOfDays:" + numDays);
        long ct = System.currentTimeMillis();
        Candle.Type t = Candle.Type.T_15_MIN;
        if (sHandleRequests.getTimePeriod() < 10) {
            t = Candle.Type.T_5_MIN;
        }
        if (sHandleRequests.getTimePeriod() > 60) {
            t = Candle.Type.T_DAILY;
        } else if (sHandleRequests.getTimePeriod() > 15) {
            t = Candle.Type.T_30_MIN;
        }
        Candle[] candles = pricing.listCandleHistory(
                insId,
                t, // candle type
                ct - numDays * _DAY, // from time (millis)
                ct, // to time (millis)
                max_count);
        //logArray("pricing.listCandleHistory:", candles);
        Candle[] candles2;
        candles2 = pricing.listCandleHistory(
                50221,
                t, // candle type
                ct - numDays * _DAY, // from time (millis)
                ct, // to time (millis)
                max_count);
        this.writeToFileArray("pricing.listCandleHistory:", candles, candles, "latestUpdate.csv");
        
        

        //this.writeToFileArray("pricing.listCandleHistory:", candles, candles2, "AUDUSDref.csv");

        //this.writeToFileArray("pricing.listCandleHistory:", candles2, candles2, "US30.csv");


        return candles;
    }

    private Quote[] callListQuotes(Pricing pricing) throws RemoteException {
        Quote[] quotes = pricing.listQuotes();
        logArray("pricing.listQuotes:", quotes);
        log();
        return quotes;
    }

    private Message[] callListNews(Messaging messaging) throws RemoteException {
        final Message[] messages = messaging.listNews();
        logArray("messaging.listNews:", messages);
        log();
        return messages;
    }

    private Instrument2[] callListAvailableInstruments(UserSupport userSupport) throws RemoteException {
        Instrument2[] availInstruments = userSupport.listAvailableInstruments2();
        logArray("userSupport.listAvailableInstruments2:", availInstruments);
        log();
        return availInstruments;
    }

    private Instrument2[] callListSelectedInstruments(UserSupport userSupport) throws RemoteException {
        Instrument2[] selectedInstruments = userSupport.listSelectedInstruments2();
        logArray("userSupport.listSelectedInstruments2:", selectedInstruments);
        log();
        return selectedInstruments;
    }

    private Account[] callListAccounts(UserSupport userSupport) throws RemoteException {
        Account[] accounts = userSupport.listAccounts();
        logArray("userSupport.listAccounts:", accounts);
        log();
        return accounts;
    }

    private AccountDetails3[] callListAccountsDetails3(UserSupport userSupport) throws RemoteException {
        AccountDetails3[] accountDetails = userSupport.listAccountDetails3();
        logArray("userSupport.listAccountDetails3:", accountDetails);
        return accountDetails;
    }

    private void callChangeSelectedInstruments(UserSupport userSupport) throws RemoteException {
        int[] instrumentsToBeDeleted = new int[]{
            InstrumentIds.USD_CHF,
            InstrumentIds.GBP_USD
        };

        int[] instrumentsToBeAdded = new int[]{
            //  InstrumentIds.USD_JPY,
            //    InstrumentIds.EUR_USD,
            InstrumentIds.GBP_AUD
        };

        int[] subscribedInstrumentIds = userSupport.changeSelectedInstruments(
                instrumentsToBeDeleted,
                instrumentsToBeAdded);
        log("userSupport.changeSelectedInstruments:");
        for (int i = 0; i < subscribedInstrumentIds.length; i++) {
            log(" " + subscribedInstrumentIds[i]);
        }
        log();
    }

    private Order[] callListIncompleteOrders(Trading trading, IAccount account) throws RemoteException {
        Order[] orders = trading.listIncompleteOrders(account.getAccountId(), InstrumentIds.USD_JPY);
        logArray("trading.listIncompleteOrders:", orders);
        log();
        return orders;
    }

    private Order[] callListOrdersHistory(UserSupport user_support, IAccount account) throws RemoteException {
        Order[] ohistory = user_support.listOrdersHistory(account.getAccountId(),
                System.currentTimeMillis() - _40_MIN, System.currentTimeMillis());
        logArray("userSupport.listOrdersHistory:", ohistory);
        log();
        return ohistory;
    }

    private Transfer[] callListTransfersHistory(UserSupport user_support, IAccount account) throws RemoteException {
        Transfer[] thistory = user_support.listTransfersHistory(account.getAccountId(),
                System.currentTimeMillis() - _DAY, System.currentTimeMillis());
        logArray("userSupport.listTransfersHistory:", thistory);
        log();
        return thistory;
    }

    private void callOrderOperations(Trading trading, UserSupport userSupport, AccountDetails3 account) throws RemoteException {

        try {
            // get current price
            Quote eur_usd = marketState.waitForQuote(InstrumentIds.EUR_USD);

            // issueing direct deal order
            long ddl_id = trading.issueDirectDealOrder(account.getAccountId(),
                    InstrumentIds.EUR_USD, Order.Operation.BUY, 100000, eur_usd.getAsk());
            log("trading.issueDirectDealOrder; ddl_id = " + ddl_id);
            log();

            // issueing stop order
            long stop_id = trading.issueStopOrder(account.getAccountId(),
                    InstrumentIds.EUR_USD, Order.Operation.BUY, 100000, eur_usd.getAsk() * 1.05d); // add 5%
            log("trading.issueStopOrder; stop_id = " + stop_id);
            log();

            // issueing limit order
            long limit_id = trading.issueLimitOrder(account.getAccountId(),
                    InstrumentIds.EUR_USD, Order.Operation.BUY, 100000, eur_usd.getAsk() * 0.95d); // substract 5%
            log("trading.issueLimitOrder; limit_id = " + limit_id);
            log();

            // get current price
            Quote usd_jpy = marketState.waitForQuote(InstrumentIds.USD_JPY);

            // issueing market order
            long market_id = trading.issueMarketOrder(account.getAccountId(),
                    InstrumentIds.USD_JPY, Order.Operation.SELL, 100000, usd_jpy.getBid());
            log("trading.issueOrder; market_id = " + market_id);
            log();

            // issueing parent + contingents
            long order_id = trading.issueParentAndContingents(account.getAccountId(),
                    Order.Type.MARKET, InstrumentIds.USD_JPY, 100000, Order.Operation.BUY, usd_jpy.getAsk(),
                    Order.Operation.SELL, usd_jpy.getBid() * 1.05,
                    Order.Operation.SELL, usd_jpy.getBid() * 0.95, false);
            log("trading.issueParentAndContingents; order_id = " + order_id);
            log();

            // issueing OCO pair
            long oco_pair_id = trading.issueOCOPair(account.getAccountId(), InstrumentIds.USD_JPY, 100000,
                    Order.Operation.BUY, usd_jpy.getAsk() * 0.95,
                    Order.Operation.BUY, usd_jpy.getAsk() * 1.05);
            log("trading.issueOCOPair; order_id = " + oco_pair_id);
            log();


            //
            // CFD Orders
            //

            ContractSizeProfile contractSize = account.getContractSizeProfile();
            Instrument2[] instruments = userSupport.listAvailableInstruments2();
            for (int i = 0; i < instruments.length; i++) {
                if (instruments[i].getType() == Instrument.Type.CFD) {
                    final int instrumentId = instruments[i].getInstrumentId();
                    final Quote quote = marketState.waitForQuote(instrumentId);
                    // issueing CFD Market order
                    long cfd_market_id = trading.issueMarketOrder(account.getAccountId(), instrumentId,
                            Order.Operation.BUY,
                            contractSize.getContractSize(instrumentId) * 2,
                            quote.getAsk());
                    log("trading.issueMarketOrder; cfd_market_id = " + cfd_market_id);

                    // issueing CFD Limit order
                    long cfd_limit_id = trading.issueLimitOrder(account.getAccountId(), instrumentId,
                            Order.Operation.SELL,
                            contractSize.getContractSize(instrumentId) * 2,
                            quote.getBid() + 5 * instruments[i].getScale()); // bid + 5 pips
                    log("trading.issueLimitOrder; cfd_limit_id = " + cfd_limit_id);
                }
            }
        } catch (OrderException oe) {
            oe.printStackTrace();
        }
    }

    private void log() {
        System.out.println();
    }

    private void log(String s) {
        System.out.println(s);
    }

    private void logArray(String title, Object[] a) {
        log(title);
        for (int i = 0; i < a.length; i++) {
            log("    " + a[i]);
        }
    }

    private void writeToFileArray(String title, Candle[] a, Candle[] ref, String fname) {
        try {

            sHandleRequests.writeArrayToOutput(title, a, ref, fname);

        } catch (Exception e) {
            log("General Exception");
            log(e.getStackTrace().toString());
            e.printStackTrace();
        }
    }

    private void logHashtable(String title, Hashtable ht) {
        log(title);
        for (Enumeration e = ht.keys(); e.hasMoreElements();) {
            Object key = e.nextElement();
            Object value = ht.get(key);
            log("    key=" + key + ", value=" + value + ", class=" + value.getClass());
        }
    }

    public static void main(String[] args) {

        PropertyConfigurator.configure("log4j.properties");
        GFTUKLogin g = new GFTUKLogin();

        (new ApiUsage()).go(g.getLoginProperty());
    }

    private boolean isInstrumentAvailable(int instrumentId) {
        return true;
    }
}
