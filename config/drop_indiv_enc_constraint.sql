-- this makes it so you can delete an encounter when it is referenced by a marked individual

alter table "MARKEDINDIVIDUAL_ENCOUNTERS" drop constraint "MARKEDINDIVIDUAL_ENCOUNTERS_FK2", add constraint "MARKEDINDIVIDUAL_ENCOUNTERS_FK2" FOREIGN KEY ("CATALOGNUMBER_EID") REFERENCES "ENCOUNTER"("CATALOGNUMBER") ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED ;
