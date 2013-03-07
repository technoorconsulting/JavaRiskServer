In order to use GFT Public API, you need to provide gateway url,
user id and password information.

Currently 2 types of url supported:

    iph://<address>:<port> (proprietary protocol built on top of http)

    iphs://<address>:<port>  (same protocol, but with encryption added).

Usage:

1. Compile the example using make.bat under MS Windows or make.sh under Unix system.

2. Edit run.bat (run.sh) to put there your valid user credentials. Read comments
   below and uncomment URL for target system.

3. Run run.bat (run.sh).

---------------------------------------------------------------------------
- Release Notes
---------------------------------------------------------------------------

v.0.7.4.4.2 Current revision
Date: July 28 2009

1. #15335 [INTGR][PUBAPI] Investigate cause 'pad block corrupted', fix, if needed

=============================================================

v.0.7.4.4.1
Date: July 6 2009

1. #15138 [INTGR][PUBAPI] Create PublicAPI release for DV Quoting connection

=============================================================

v.0.7.4.4
Date: March 17 2009

1. #13904 Add new method for closing tickets @MKT

=============================================================

v.0.7.4.2
Date: Feb 27 2009

1.#13708 fpl, mrq and available equity calculation is wrong for cfd/sb products

=============================================================

v.0.7.3
Date: Oct 04 2006

1. #7754 PublicAPI: Add news to PublicAPI
2. #7969 PublicAPI: compatibility with Java 1.1
3. InstrumentIds updated

=============================================================

v.0.7.2
Date: Sep 13 2006

1. #7698 PublicAPI: MultipleLoginType
2. #7755 PublicAPI: Add statistics tracking for non-GUI logins
3. #7754 PublicAPI: Add news to PublicAPI
4. usage example updated

=============================================================

v.0.7.1
Date: July 25 2006

1. MaximumDealSizeProfile added
2. #7361  Public API: CFD/SB logic modification

=============================================================

v.0.7.0
Date: Mar 03 2006

1. #5509 Public API: listCandleHistory issue
2. #5556 Public API: API problems
3. set of issueXXXOrder methods now contains instumentId as parameter
4. #5795 Public API: modify Public API so it works with CFD/SB instruments and profiles

=============================================================

v.0.6.8
Date: Jan 13 2006

1. issueParentAndContingents method implemented
2. usage example updated
3. rif.jar and crypto.jar updated
4. new methods of MarketState class implemented
5. issueOCOPair method implemented
6. problem with property permission(java.lang.reflect.ReflectPermission suppressAccessChecks) fixed
7. #4270 CLOSE orders in Public API
8. #5508 Public API: Issue LIMIT Order

=============================================================

v.0.6.7
Date: Nov 15 2005

1. issueOrder method marked deprecated
2. a set of issue<XXX> methods added for simplification of the API
3. some async messages now contain instrument symbol along with instrument_id
4. #5098  Public API: add 1 min aggregates
5. usage example updated

=============================================================

v.0.6.6
Date: May 31 2005

Shell scripts fixed. Example can now run Ok under Unix.

=============================================================

v.0.6.5
Date: May 18 2005

RIF and Storable frameworks are updated: compatibility with the new
server release achieved.
New utility library added.

=============================================================

v.0.6.3
Date: March 17 2005

RIF and Storable frameworks are updated: errors handling improved. If there is a
business exception while invoking method remotely, business exception is returned
back to the caller. If there is unknown exception, it is wrapped into the StorableException
and serialized back to the client, otherwise, internal server error is raised.
