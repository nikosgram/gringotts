package org.gestern.gringotts;

import java.io.IOException;

import io.ebean.annotation.Platform;
import io.ebean.dbmigration.DbMigration;

public class Migration {
    /**
     * Generates files used to update Ebean storage when data models are changed
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {

        DbMigration dbMigration = DbMigration.create();
        dbMigration.setPlatform(Platform.SQLITE);

        dbMigration.generateMigration();
    }
}
