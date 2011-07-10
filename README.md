What is frottle
===============

Frottle is a collection of examples for throttling in java. Throttling can be useful in systems with high load, like a web application, or middleware.

What is throttling
==================
Throttling is any behavior that limits the total amount of work processed regardless of how much work is offered, *without crashing* the system. The key to throttling is to do something other than regularly processing the work. The alternative process should need much less resources. For example a web site might still render headers and footers, but render a "Sorry, too busy" message instead of user content to relieve the database.

Why trottling
=============
Built in behavior in overload scenarios can protect a system from failing badly. Without throttling systems can become unpredictable, severely crippled, or totally hung. This happening on production can severely impact user experience and business. A calculated and tested scenario is typically preferred over a surprise.

How do I use this
==================
For now just check out the code and look at the testcases. Frottle is more like a set of recipes than a framework. 

License?
========
Meh, licenses are boring. Feel free to copy all you like. I generally like it if you leave the author tags, but I have no intention to enforce anything.