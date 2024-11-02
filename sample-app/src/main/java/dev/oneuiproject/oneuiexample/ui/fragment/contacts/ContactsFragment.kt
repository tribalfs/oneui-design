package dev.oneuiproject.oneuiexample.ui.fragment.contacts

import android.content.Context
import android.content.res.Configuration
import android.database.MatrixCursor
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.ImageView
import android.widget.SectionIndexer
import android.widget.TextView
import androidx.appcompat.util.SeslRoundedCorner
import androidx.appcompat.util.SeslSubheaderRoundedCorner
import androidx.appcompat.view.menu.SeslMenuItem
import androidx.core.view.MenuProvider
import androidx.indexscroll.widget.SeslCursorIndexer
import androidx.indexscroll.widget.SeslIndexScrollView
import androidx.indexscroll.widget.SeslIndexScrollView.OnIndexBarEventListener
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sec.sesl.tester.R
import dev.oneuiproject.oneui.utils.IndexScrollUtils
import dev.oneuiproject.oneui.widget.Separator
import dev.oneuiproject.oneuiexample.ui.core.base.BaseFragment

class ContactsFragment : BaseFragment() {
    private var mCurrentSectionIndex = 0
    private var mIndexScrollView: SeslIndexScrollView? = null
    private var mContactsListRv: RecyclerView? = null

    private var mIsTextModeEnabled = false
    private var mIsIndexBarPressed = false
    private val mHideIndexBar = Runnable { IndexScrollUtils.animateVisibility(mIndexScrollView!!, false) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mIndexScrollView = view.findViewById(R.id.indexscroll_view)
        initRecyclerView(view)
    }

    override fun onHiddenChanged(hidden: Boolean) {
        if (!hidden) {
            requireActivity().addMenuProvider(
                menuProvider,
                viewLifecycleOwner,
                Lifecycle.State.STARTED
            )
        } else {
            requireActivity().removeMenuProvider(menuProvider)
        }
    }

    private val menuProvider: MenuProvider = object : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.menu_contacts_list, menu)

            val menuItem = menu.findItem(R.id.menu_indexscroll_text)
            (menuItem as SeslMenuItem).badgeText = "1"
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            if (menuItem.itemId == R.id.menu_indexscroll_text) {
                mIsTextModeEnabled = !mIsTextModeEnabled
                if (mIsTextModeEnabled) {
                    menuItem.setTitle("Hide letters")
                } else {
                    menuItem.setTitle("Show letters")
                }
                (menuItem as SeslMenuItem).badgeText = null
                mIndexScrollView!!.apply {
                    setIndexBarTextMode(mIsTextModeEnabled)
                    invalidate()
                }
                return true
            }
            return false
        }
    }


    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        val isRtl = newConfig
            .layoutDirection == View.LAYOUT_DIRECTION_RTL
        if (mIndexScrollView != null) {
            mIndexScrollView!!.setIndexBarGravity(
                if (isRtl)
                    SeslIndexScrollView.GRAVITY_INDEX_BAR_LEFT
                else
                    SeslIndexScrollView.GRAVITY_INDEX_BAR_RIGHT
            )
        }
    }

    override fun getLayoutResId(): Int = R.layout.fragment_contacts

    override fun getIconResId(): Int = dev.oneuiproject.oneui.R.drawable.ic_oui_contact_outline

    override fun getTitle(): CharSequence = "Contacts"

    private fun initRecyclerView(view: View) {
        mContactsListRv = view.findViewById<RecyclerView?>(R.id.contacts_list).apply {
            setLayoutManager(LinearLayoutManager(mContext))
            setAdapter(ContactsAdapter())
            addItemDecoration(ItemDecoration(mContext))
            setItemAnimator(null)
            seslSetFillBottomEnabled(true)
            seslSetLastRoundedCorner(true)
            seslSetIndexTipEnabled(true)
            seslSetGoToTopEnabled(true)
            seslSetSmoothScrollEnabled(true)
        }
    }

    private fun initIndexScroll() {
        val isRtl = resources.configuration
            .layoutDirection == View.LAYOUT_DIRECTION_RTL

        mIndexScrollView!!.setIndexBarGravity(
            if (isRtl)
                SeslIndexScrollView.GRAVITY_INDEX_BAR_LEFT
            else
                SeslIndexScrollView.GRAVITY_INDEX_BAR_RIGHT
        )

        val cursor = MatrixCursor(arrayOf("item"))
        for (item in listItems) {
            cursor.addRow(arrayOf(item))
        }

        cursor.moveToFirst()

        val indexer = SeslCursorIndexer(cursor, 0,
            "A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P,Q,R,S,T,U,V,W,X,Y,Z,Б".split(",".toRegex())
                .dropLastWhile { it.isEmpty() }.toTypedArray(), 0
        )
        indexer.setGroupItemsCount(1)
        indexer.setMiscItemsCount(3)

        mIndexScrollView!!.setIndexer(indexer)
        mIndexScrollView!!.setOnIndexBarEventListener(
            object : OnIndexBarEventListener {
                override fun onIndexChanged(sectionIndex: Int) {
                    if (mCurrentSectionIndex != sectionIndex) {
                        mCurrentSectionIndex = sectionIndex
                        if (mListView!!.scrollState != RecyclerView.SCROLL_STATE_IDLE) {
                            mListView!!.stopScroll()
                        }
                        (mListView!!.layoutManager as LinearLayoutManager)
                            .scrollToPositionWithOffset(sectionIndex, 0)
                    }
                }

                override fun onPressed(v: Float) {
                    mIsIndexBarPressed = true
                    mListView!!.removeCallbacks(mHideIndexBar)
                }

                override fun onReleased(v: Float) {
                    mIsIndexBarPressed = false
                    if (mListView!!.scrollState == RecyclerView.SCROLL_STATE_IDLE) {
                        mListView!!.postDelayed(mHideIndexBar, 1500)
                    }
                }
            })
        mIndexScrollView!!.attachToRecyclerView(mListView)
        mListView!!.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE
                    && !mIsIndexBarPressed
                ) {
                    recyclerView.postDelayed(mHideIndexBar, 1500)
                } else {
                    mListView!!.removeCallbacks(mHideIndexBar)
                    IndexScrollUtils.animateVisibility(mIndexScrollView!!, true)
                }
            }
        })
    }

    inner class ContactsAdapter internal constructor() :
        RecyclerView.Adapter<ContactsAdapter.ViewHolder>(),
        SectionIndexer {
        var mSections: MutableList<String> = ArrayList()
        var mPositionForSection: MutableList<Int> = ArrayList()
        var mSectionForPosition: MutableList<Int> = ArrayList()

        init {
            mSections.add("")
            mPositionForSection.add(0)
            mSectionForPosition.add(0)

            for (i in 1 until listItems.size) {
                val letter = listItems[i]
                if (letter.length == 1) {
                    mSections.add(letter)
                    mPositionForSection.add(i)
                }
                mSectionForPosition.add(mSections.size - 1)
            }
        }

        override fun getItemCount(): Int {
            return listItems.size
        }

        override fun getItemViewType(position: Int): Int {
            return if ((listItems[position].length == 1)) 1 else 0
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            if (viewType == 0) {
                val inflater = LayoutInflater.from(mContext)
                val view = inflater.inflate(
                    R.layout.sample3_view_indexscroll_listview_item, parent, false
                )
                return ViewHolder(view, false)
            } else {
                return ViewHolder(Separator(mContext), true)
            }
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            if (holder.isSeparator) {
                holder.textView!!.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                )
            } else {
                if (position == 0) {
                    holder.imageView!!.setImageResource(R.drawable.indexscroll_group_icon)
                } else {
                    holder.imageView!!.setImageResource(R.drawable.indexscroll_item_icon)
                }
            }
            holder.textView!!.text = listItems[position]
        }

        override fun getSections(): Array<Any> {
            return mSections.toTypedArray()
        }

        override fun getPositionForSection(sectionIndex: Int): Int {
            return mPositionForSection[sectionIndex]
        }

        override fun getSectionForPosition(position: Int): Int {
            return mSectionForPosition[position]
        }

        inner class ViewHolder internal constructor(itemView: View, var isSeparator: Boolean) :
            RecyclerView.ViewHolder(itemView) {
            var imageView: ImageView? = null
            var textView: TextView? = null

            init {
                if (isSeparator) {
                    textView = itemView as TextView
                } else {
                    imageView = itemView.findViewById(R.id.indexscroll_list_item_icon)
                    textView = itemView.findViewById(R.id.indexscroll_list_item_text)
                }
            }
        }
    }

    private inner class ItemDecoration(context: Context) : RecyclerView.ItemDecoration() {
        private val mDivider: Drawable?
        private val mRoundedCorner: SeslSubheaderRoundedCorner

        init {
            val outValue = TypedValue()
            context.theme.resolveAttribute(androidx.appcompat.R.attr.isLightTheme, outValue, true)

            mDivider = context.getDrawable(
                if (outValue.data == 0)
                    androidx.appcompat.R.drawable.sesl_list_divider_dark
                else
                    androidx.appcompat.R.drawable.sesl_list_divider_light
            )

            mRoundedCorner = SeslSubheaderRoundedCorner(mContext)
            mRoundedCorner.roundedCorners = SeslRoundedCorner.ROUNDED_CORNER_ALL
        }

        override fun onDraw(
            c: Canvas, parent: RecyclerView,
            state: RecyclerView.State
        ) {
            super.onDraw(c, parent, state)

            for (i in 0 until parent.childCount) {
                val child = parent.getChildAt(i)
                val holder = mContactsListRv!!.getChildViewHolder(child) as ContactsAdapter.ViewHolder
                if (!holder.isSeparator) {
                    val top = (child.bottom
                            + (child.layoutParams as MarginLayoutParams).bottomMargin)
                    val bottom = mDivider!!.intrinsicHeight + top

                    mDivider.setBounds(parent.left, top, parent.right, bottom)
                    mDivider.draw(c)
                }
            }
        }

        override fun seslOnDispatchDraw(
            c: Canvas,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            for (i in 0 until parent.childCount) {
                val child = parent.getChildAt(i)
                val holder = mContactsListRv!!.getChildViewHolder(child) as ContactsAdapter.ViewHolder
                if (holder.isSeparator) {
                    mRoundedCorner.drawRoundedCorner(child, c)
                }
            }
        }
    }


    var listItems: Array<String> = arrayOf(
        "Groups",
        "A",
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
        "B",
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
        "C",
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
        "D",
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
        "E",
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
        "F",
        "Fawn",
        "Felix",
        "Fern",
        "Fernando",
        "Finn",
        "Francis",
        "G",
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
        "H",
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
        "I",
        "Imogen",
        "Irene",
        "Isaac",
        "Isaiah",
        "J",
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
        "K",
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
        "L",
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
        "M",
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
        "N",
        "Nadeen",
        "Naomi",
        "Natalie",
        "Naveen",
        "Neil",
        "Nevin",
        "Nicolas",
        "Noah",
        "Noel",
        "O",
        "Ocean",
        "Olive",
        "Oliver",
        "Oren",
        "Orlando",
        "Oscar",
        "P",
        "Paul",
        "Payten",
        "Porter",
        "Preston",
        "Q",
        "Quintin",
        "R",
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
        "S",
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
        "T",
        "Tanner",
        "Tavian",
        "Taye",
        "Taylore",
        "Thomas",
        "Timothy",
        "Tobias",
        "Trevor",
        "Trey",
        "Tristan",
        "Troy",
        "Tyson",
        "U",
        "Ulvi",
        "Uwu",
        "V",
        "Vanessa",
        "Varian",
        "Verena",
        "Vernon",
        "Vincent",
        "Viola",
        "Vivian",
        "W",
        "Wade",
        "Warren",
        "Will",
        "William",
        "X",
        "Xavier",
        "Y",
        "Yann :)",
        "Z",
        "Zachary",
        "Zane",
        "Zion",
        "Zoe",
        "Б",
        "Блять lol",
        "#",
        "040404",
        "121002"
    )
}
