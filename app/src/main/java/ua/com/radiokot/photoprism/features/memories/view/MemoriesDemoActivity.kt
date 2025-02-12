package ua.com.radiokot.photoprism.features.memories.view

import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import ua.com.radiokot.photoprism.base.view.BaseActivity
import ua.com.radiokot.photoprism.databinding.ActivityMemoriesDemoBinding
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.gallery.data.storage.SimpleGalleryMediaRepository
import ua.com.radiokot.photoprism.features.gallery.search.albums.view.model.AlbumListItem
import ua.com.radiokot.photoprism.features.memories.data.model.Memory
import ua.com.radiokot.photoprism.features.memories.data.storage.MemoriesRepository
import ua.com.radiokot.photoprism.features.memories.logic.GetMemoriesUseCase
import ua.com.radiokot.photoprism.features.viewer.view.MediaViewerActivity
import ua.com.radiokot.photoprism.util.LocalDate
import java.util.Calendar

/**
 * This is only for demo purposes.
 */
class MemoriesDemoActivity : BaseActivity() {
    private val log = kLogger("MemoriesDemoActivity")

    private lateinit var view: ActivityMemoriesDemoBinding

    private val repository: MemoriesRepository by inject()
    private val adapter = ItemAdapter<AlbumListItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        view = ActivityMemoriesDemoBinding.inflate(layoutInflater)
        setContentView(view.root)

        setSupportActionBar(view.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (savedInstanceState == null) {
            addMemories()
        }

        subscribeToRepository()
        initList()
    }

    private fun addMemories() {
        get<GetMemoriesUseCase>()
            .invoke()
            .flatMapCompletable(repository::add)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                view.statusTextView.text = "Loading..."
            }
            .doOnError {
                view.statusTextView.text = "Failed loading: $it"
                log.error("failed_loading_memories", it)
            }
            .subscribeBy {
                view.statusTextView.text = "Done"
            }
            .autoDispose(this)
    }

    private fun subscribeToRepository() {
        repository
            .items
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy { memories ->
                val currentYear = LocalDate().getCalendar()[Calendar.YEAR]

                adapter.set(memories.map { memory ->
                    AlbumListItem(
                        title = when (memory) {
                            is Memory.ThisDayInThePast ->
                                "${currentYear - memory.year} years ago"
                        },
                        thumbnailUrl = memory.smallThumbnailUrl,
                        isAlbumSelected = false,
                        source = null,
                    )
                })
            }
            .autoDispose(this)
    }

    private fun initList() {
        val listAdapter = FastAdapter.with(adapter).apply {
            stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY

            onClickListener = { _, _, _, position: Int ->
                startActivity(
                    Intent(this@MemoriesDemoActivity, MediaViewerActivity::class.java)
                        .putExtras(
                            MediaViewerActivity.getBundle(
                                mediaIndex = 0,
                                repositoryParams = SimpleGalleryMediaRepository.Params(
                                    query = repository.itemsList[position].searchQuery,
                                ),
                                areActionsEnabled = true
                            )
                        )
                )
                true
            }
        }

        with(view.memoriesRecyclerView) {
            adapter = listAdapter
            // Layout manager is set in XML.
        }
    }
}
