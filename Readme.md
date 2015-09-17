#Quack

NXT atomic swap engine (Java).

#How to build

Use Eclipse IDE

#API

Send POST request to http://localhost:17779/api

Put api calls in POST body.

##quackInit

Initiate quack transfer.

requestType=init&secret=X&recipient=Y&finishheight=Z&assets=[{"id":"17091401215301664836","QNT":5,"type":"A"},{"id":"1","QNT":100000000,"type":"NXT"}]&expected_assets=[{"id":"1","QNT":100000000,"type":"NXT"}]&private_message=P

X - secret phrase
Y - recipient account in RS format
Z - phased finish height
P - encrypted message for recipient (optional)

assets contain information about transferred assets and NXT in JSON format.
expected_assets contain information for recipient and can be used for validation

##quackAccept

Accept quack transfer.

requestType=accept&secret=X&recipient=Y&finishheight=Z&assets=[{"id":"1","QNT":100000000,"type":"NXT"}]&triggerhash=N

X - secret phrase
Y - recipient account in RS format
Z - finish height (ideally all transactions should have the same finish height)
N - trigger hash. It can be found in every phased quack transaction as linked hash. Also available with quackScan call.

assets - transferred assets.

##quackTrigger

Finalize quack transfer.

requestType=trigger&secret=X&triggerBytes=M

X - secret phrase
M - unsigned bytes of trigger transaction. Available with quackScan call.

##quackScan

Check incoming quack requests and current status.

requestType=scan&account=X&timelimit=Y

X - my account in RS format
Y - how old transactions are scanned. Time in seconds.