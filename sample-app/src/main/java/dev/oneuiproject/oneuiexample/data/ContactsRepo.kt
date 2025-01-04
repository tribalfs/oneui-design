package dev.oneuiproject.oneuiexample.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

data class Contact(
    val name: String,
    val number: String
)

enum class ActionModeSearch{
    DISMISS,
    NO_DISMISS,
    CONCURRENT
}

data class ContactsSettings(
    val isTextModeIndexScroll: Boolean = false,
    val autoHideIndexScroll: Boolean = true,
    val searchOnActionMode: ActionModeSearch = ActionModeSearch.DISMISS,
    val actionModeShowCancel: Boolean = false
)

class ContactsRepo (context: Context) {

    private val dataStore: DataStore<Preferences> = context.sampleAppPreferences

    val contactsFlow: Flow<List<Contact>>  = flow {
        emit(
            personsList.map {
                Contact(
                    name = it,
                    number = "+63" + (1000000000..9999999999).random().toString()
                )
            }
        )
    }


    suspend fun setIndexScrollMode(isTextMode: Boolean){
        dataStore.edit {
            it[PREF_KEY_CONTACT_INDEXSCROLL_TEXT_MODE] = isTextMode
        }
    }

    suspend fun setIndexScrollAutoHide(autoHide: Boolean){
        dataStore.edit {
            it[PREF_KEY_CONTACT_INDEXSCROLL_AUTO_HIDE] = autoHide
        }
    }

    suspend fun toggleKeepSearch(){
        dataStore.edit {
            it[PREF_KEY_CONTACT_ACTIONMODE_KEEP_SEARCH] = when(contactsSettingsFlow.first().searchOnActionMode){
                ActionModeSearch.DISMISS -> ActionModeSearch.NO_DISMISS.ordinal
                ActionModeSearch.NO_DISMISS -> ActionModeSearch.CONCURRENT.ordinal
                ActionModeSearch.CONCURRENT -> ActionModeSearch.DISMISS.ordinal
            }
        }
    }

    suspend fun toggleShowCancel(){
        dataStore.edit {
            it[PREF_KEY_CONTACT_ACTIONMODE_SHOW_CANCEL] = !contactsSettingsFlow.first().actionModeShowCancel
        }
    }

    val contactsSettingsFlow: Flow<ContactsSettings>  = dataStore.data.map {
        ContactsSettings(
            it[PREF_KEY_CONTACT_INDEXSCROLL_TEXT_MODE] ?: false,
            it[PREF_KEY_CONTACT_INDEXSCROLL_AUTO_HIDE] ?: true,
            ActionModeSearch.entries[it[PREF_KEY_CONTACT_ACTIONMODE_KEEP_SEARCH] ?: 0],
            it[PREF_KEY_CONTACT_ACTIONMODE_SHOW_CANCEL] ?: false
        )
    }

    private companion object{
        val PREF_KEY_CONTACT_INDEXSCROLL_TEXT_MODE = booleanPreferencesKey("indexScrollTextMode")
        val PREF_KEY_CONTACT_INDEXSCROLL_AUTO_HIDE = booleanPreferencesKey("indexScrollAutoHide")
        val PREF_KEY_CONTACT_ACTIONMODE_KEEP_SEARCH = intPreferencesKey("actionModeSearch")
        val PREF_KEY_CONTACT_ACTIONMODE_SHOW_CANCEL = booleanPreferencesKey("actionModeShowCancel")
    }
}


private val personsList = listOf(
    "Aaron",
    "Abe",
    "Abigail",
    "Abraham",
    "Ace",
    "Adelaide",
    "Adele",
    "Aiden",
    "Alice",
    "Allison",
    "Amelia",
    "Amity",
    "Anise",
    "Ann",
    "Annabel",
    "Anneliese",
    "Annora",
    "Anthony",
    "Apollo",
    "Arden",
    "Arthur",
    "Aryn",
    "Ashten",
    "Avery",
    "Bailee",
    "Bailey",
    "Beck",
    "Benjamin",
    "Berlynn",
    "Bernice",
    "Bianca",
    "Blair",
    "Blaise",
    "Blake",
    "Blanche",
    "Blayne",
    "Bram",
    "Brandt",
    "Bree",
    "Breean",
    "Brendon",
    "Brett",
    "Brighton",
    "Brock",
    "Brooke",
    "Byron",
    "Caleb",
    "Cameron",
    "Candice",
    "Caprice",
    "Carelyn",
    "Caren",
    "Carleen",
    "Carlen",
    "Carmden",
    "Cash",
    "Caylen",
    "Cerise",
    "Charles",
    "Chase",
    "Clark",
    "Claude",
    "Claudia",
    "Clelia",
    "Clementine",
    "Cody",
    "Conrad",
    "Coralie",
    "Coreen",
    "Coy",
    "Damien",
    "Damon",
    "Daniel",
    "Dante",
    "Dash",
    "David",
    "Dawn",
    "Dean",
    "Debree",
    "Denise",
    "Denver",
    "Devon",
    "Dex",
    "Dezi",
    "Dominick",
    "Doran",
    "Drake",
    "Drew",
    "Dustin",
    "Edward",
    "Elein",
    "Eli",
    "Elias",
    "Elijah",
    "Ellen",
    "Ellice",
    "Ellison",
    "Ellory",
    "Elodie",
    "Eloise",
    "Emeline",
    "Emerson",
    "Eminem",
    "Erin",
    "Evelyn",
    "Everett",
    "Evony",
    "Fawn",
    "Felix",
    "Fern",
    "Fernando",
    "Finn",
    "Francis",
    "Gabriel",
    "Garrison",
    "Gavin",
    "George",
    "Georgina",
    "Gillian",
    "Glenn",
    "Grant",
    "Gregory",
    "Grey",
    "Gwendolen",
    "Haiden",
    "Harriet",
    "Harrison",
    "Heath",
    "Henry",
    "Hollyn",
    "Homer",
    "Hope",
    "Hugh",
    "Hyrum",
    "Imogen",
    "Irene",
    "Isaac",
    "Isaiah",
    "Jack",
    "Jacklyn",
    "Jackson",
    "Jae",
    "Jaidyn",
    "James",
    "Jane",
    "Janetta",
    "Jared",
    "Jasper",
    "Javan",
    "Jax",
    "Jeremy",
    "Joan",
    "Joanna",
    "Jolee",
    "Jordon",
    "Joseph",
    "Josiah",
    "Juan",
    "Judd",
    "Jude",
    "Julian",
    "Juliet",
    "Julina",
    "June",
    "Justice",
    "Justin",
    "Kae",
    "Kai",
    "Kaitlin",
    "Kalan",
    "Karilyn",
    "Kate",
    "Kathryn",
    "Kent",
    "Kingston",
    "Korin",
    "Krystan",
    "Kylie",
    "Lane",
    "Lashon",
    "Lawrence",
    "Lee",
    "Leo",
    "Leonie",
    "Levi",
    "Lilibeth",
    "Lillian",
    "Linnea",
    "Louis",
    "Louisa",
    "Love",
    "Lucinda",
    "Luke",
    "Lydon",
    "Lynn",
    "Madeleine",
    "Madisen",
    "Mae",
    "Malachi",
    "Marcella",
    "Marcellus",
    "Marguerite",
    "Matilda",
    "Matteo",
    "Meaghan",
    "Merle",
    "Michael",
    "Menime",
    "Mirabel",
    "Miranda",
    "Miriam",
    "Monteen",
    "Murphy",
    "Myron",
    "Nadeen",
    "Naomi",
    "Natalie",
    "Naveen",
    "Neil",
    "Nevin",
    "Nicolas",
    "Noah",
    "Noel",
    "Ocean",
    "Olive",
    "Oliver",
    "Oren",
    "Orlando",
    "Oscar",
    "Paul",
    "Payten",
    "Porter",
    "Preston",
    "Quintin",
    "Raine",
    "Randall",
    "Raven",
    "Ray",
    "Rayleen",
    "Reagan",
    "Rebecca",
    "Reese",
    "Reeve",
    "Rene",
    "Rhett",
    "Ricardo",
    "Riley",
    "Robert",
    "Robin",
    "Rory",
    "Rosalind",
    "Rose",
    "Ryder",
    "Rylie",
    "Salvo :)",
    "Sean",
    "Selene",
    "Seth",
    "Shane",
    "Sharon",
    "Sheridan",
    "Sherleen",
    "Silvia",
    "Sophia",
    "Sue",
    "Sullivan",
    "Susannah",
    "Sutton",
    "Suzan",
    "Syllable",
    "Tanner",
    "Tavian",
    "Taye",
    "Taylore",
    "Thomas",
    "Timothy",
    "Tobias",
    "Trevor",
    "Trey",
    "Tribalfs",
    "Tristan",
    "Troy",
    "Tyson",
    "Ulvi",
    "Uwu",
    "Vanessa",
    "Varian",
    "Verena",
    "Vernon",
    "Vincent",
    "Viola",
    "Vivian",
    "Wade",
    "Warren",
    "Will",
    "William",
    "Xavier",
    "Yann :)",
    "Zachary",
    "Zane",
    "Zion",
    "Zoe",
    "Блять lol",
    "040404",
    "121002"
)


