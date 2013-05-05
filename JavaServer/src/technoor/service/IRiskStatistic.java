/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package technoor.service;

import technoor.datarequest.ProtoTradeApi;
import technoor.datarequest.ProtoTradeApi.CandleProto;

/**
 *
 * @author salman
 */
public interface IRiskStatistic {

    CandleProto [] calculateStat(ProtoTradeApi.CandleProto[] p);
    
}
