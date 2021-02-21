# investment-portfolio
My attempt for a functional rebalancer of a fund.

# requirements
## quantity weights with tolerance
A fund should be composed of a number of instruments
and contain fixed proportions of them. It can deviate within a tolerance.
## minimal transactions
The number of transactions should be minimized. This is done
for ease of reporting and for tax reasons (no buy 2 sell 1 as the sell can realise taxable profit).

Transactions happen on a control account, the client accounts do not touch the market.

Market orders should be also minimal as charges can accumulate on the control account. (settlement costs where no netting is taking place)
## accounts cross
If an account is selling and another is buying they can transact with each other.
The price of that transaction if fixed at the mid of the best quote Buy and best quote Sell.
This will not change even of trading on those quotes is refused. If no trading takes place and we abort,
the crossing trade will not take place.
## net sell before net buy
Sell on the market before buying, so that any slippage will not leave us short.

## fractional trading
Allocate fractions of an instrument quantity.

Any excess quantity is stored in a fractional account, that can be used to top-up market orders.

The fractional account will trade at best quote on the direction trading.

## rebalancing
Periodically the target weights change. Or instruments might be added or removed.

## divest only instruments
periodically instruments are replaced. Optionally the old instrument is kept, but it is the one sold first.
