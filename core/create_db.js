db.getSiblingDB('flow_db')
db.getSiblingDB('flow_db_ut')

use flow_db
db.createUser(
    {
        user: 'flowci',
        pwd: 'flowci',
        roles: [{ role: 'readWrite', db: 'flow_db' }],
    },
);

use flow_db_ut
db.createUser(
    {
        user: 'flowci_ut',
        pwd: 'flowci_ut',
        roles: [{ role: 'readWrite', db: 'flow_db_ut' }],
    },
);