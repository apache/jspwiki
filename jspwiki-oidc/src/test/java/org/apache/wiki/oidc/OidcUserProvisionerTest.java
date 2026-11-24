/*
    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.
 */
package org.apache.wiki.oidc;

import org.apache.wiki.auth.NoSuchPrincipalException;
import org.apache.wiki.auth.user.UserDatabase;
import org.apache.wiki.auth.user.UserProfile;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.pac4j.core.profile.CommonProfile;

class OidcUserProvisionerTest {

    private UserDatabase database;
    private UserProfile created;

    @BeforeEach
    void emptyDatabase() throws Exception {
        database = Mockito.mock( UserDatabase.class );
        Mockito.when( database.findByLoginName( Mockito.anyString() ) ).thenThrow( new NoSuchPrincipalException( "none" ) );
        Mockito.when( database.findByFullName( Mockito.anyString() ) ).thenThrow( new NoSuchPrincipalException( "none" ) );
        Mockito.when( database.findByWikiName( Mockito.anyString() ) ).thenThrow( new NoSuchPrincipalException( "none" ) );
        created = Mockito.mock( UserProfile.class );
        Mockito.when( database.newProfile() ).thenReturn( created );
    }

    private static CommonProfile identity( final String... attributes ) {
        final CommonProfile profile = new CommonProfile();
        profile.setId( "subject-1" );
        for( int i = 0; i < attributes.length; i += 2 ) {
            profile.addAttribute( attributes[ i ], attributes[ i + 1 ] );
        }
        return profile;
    }

    private UserProfile saved() throws Exception {
        Mockito.verify( database ).save( created );
        return created;
    }

    @Test
    void createsTheProfileWithTheNameAndTheEmailOfTheProvider() throws Exception {
        Assertions.assertTrue( OidcUserProvisioner.provision( database, "kcuser", identity( "name", "Ana Perez", "email", "ana@example.org" ) ) );

        final UserProfile profile = saved();
        Mockito.verify( profile ).setLoginName( "kcuser" );
        Mockito.verify( profile ).setFullname( "Ana Perez" );
        Mockito.verify( profile ).setEmail( "ana@example.org" );
        Mockito.verify( profile, Mockito.never() ).setPassword( Mockito.any() );
    }

    @Test
    void leavesAnExistingProfileAsItIs() throws Exception {
        Mockito.doReturn( Mockito.mock( UserProfile.class ) ).when( database ).findByLoginName( "kcuser" );

        Assertions.assertFalse( OidcUserProvisioner.provision( database, "kcuser", identity( "name", "Ana Perez" ) ) );

        Mockito.verify( database, Mockito.never() ).save( Mockito.any() );
    }

    @Test
    void usesTheGivenAndTheFamilyNameWhenThereIsNoName() throws Exception {
        OidcUserProvisioner.provision( database, "kcuser", identity( "given_name", "Ana", "family_name", "Perez" ) );

        Mockito.verify( saved() ).setFullname( "Ana Perez" );
    }

    @Test
    void usesTheLoginNameWhenTheProviderSendsNoName() throws Exception {
        OidcUserProvisioner.provision( database, "kcuser", identity() );

        final UserProfile profile = saved();
        Mockito.verify( profile ).setFullname( "kcuser" );
        Mockito.verify( profile, Mockito.never() ).setEmail( Mockito.any() );
    }

    @Test
    void usesTheLoginNameWhenAnotherUserHasTheFullName() throws Exception {
        Mockito.doReturn( Mockito.mock( UserProfile.class ) ).when( database ).findByFullName( "Ana Perez" );

        OidcUserProvisioner.provision( database, "kcuser", identity( "name", "Ana Perez" ) );

        Mockito.verify( saved() ).setFullname( "kcuser" );
    }

    @Test
    void createsNothingWhenEvenTheLoginNameIsTakenAsAName() throws Exception {
        Mockito.doReturn( Mockito.mock( UserProfile.class ) ).when( database ).findByFullName( Mockito.anyString() );

        Assertions.assertFalse( OidcUserProvisioner.provision( database, "kcuser", identity( "name", "Ana Perez" ) ) );

        Mockito.verify( database, Mockito.never() ).save( Mockito.any() );
    }

    @Test
    void createsNothingWithoutALoginName() throws Exception {
        Assertions.assertFalse( OidcUserProvisioner.provision( database, " ", identity( "name", "Ana Perez" ) ) );

        Mockito.verify( database, Mockito.never() ).save( Mockito.any() );
    }

}
