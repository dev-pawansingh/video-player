package com.pawansingh.vp.update

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.pawansingh.vp.R

class ShortsFragment : Fragment() {

    private lateinit var viewPager: ViewPager2
    private lateinit var adapter: ShortsAdapter
    private var player: ExoPlayer? = null
    private var sharedPlayerView: PlayerView? = null
    private val items = mutableListOf<ShortsModel>()
    private var currentViewCountedVideoId: String? = null
    private val viewedVideosSession = mutableSetOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_shorts, container, false)
    }

    private val signupLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            val pos = result.data?.getIntExtra("shorts_return_position", -1) ?: -1
            if (pos != -1) {
                viewPager.post {
                    viewPager.setCurrentItem(pos, false)
                    playAt(pos)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        viewPager = view.findViewById(R.id.shortsViewPager)

        adapter = ShortsAdapter(
            items,
            onInteraction = { interaction ->
                handleInteraction(interaction)
            }
        )

        viewPager.adapter = adapter
        viewPager.orientation = ViewPager2.ORIENTATION_VERTICAL
        viewPager.offscreenPageLimit = 1

        initPlayer()
        sharedPlayerView = PlayerView(requireContext()).apply {
            useController = false
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        viewPager.registerOnPageChangeCallback(pageCallback)

        loadVideosFromFirestore()
        syncLikedStates()
    }

    private fun buildDataSourceFactory(): CacheDataSource.Factory {
        val cache = VideoCache.get(requireContext())
        val http = DefaultHttpDataSource.Factory()
        return CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(http)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    private fun createMediaSource(url: String) =
        ProgressiveMediaSource.Factory(buildDataSourceFactory())
            .createMediaSource(MediaItem.fromUri(url))

    private fun initPlayer() {
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(1500, 5000, 1000, 1500)
            .build()

        player = ExoPlayer.Builder(requireContext())
            .setLoadControl(loadControl)
            .build().apply {
                playWhenReady = true
                repeatMode = Player.REPEAT_MODE_ONE
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        if (state == Player.STATE_READY) {
                            hideLoadingForCurrentItem()
                            val videoId = currentViewCountedVideoId ?: return
                            if (viewedVideosSession.contains(videoId)) return
                            viewedVideosSession.add(videoId)
                            addViewOnce(videoId) {
                                val index = items.indexOfFirst { it.id == videoId }
                                if (index == -1) return@addViewOnce

                                items[index].views += 1

                                val recycler = viewPager.getChildAt(0) as? RecyclerView ?: return@addViewOnce
                                val vh = recycler.findViewHolderForAdapterPosition(index) ?: return@addViewOnce
                                val tvViews = vh.itemView.findViewById<TextView>(R.id.tvViews)
                                tvViews.text = "${adapter.formatCount(items[index].views)} views"
                            }
                        }
                    }
                    override fun onPlayerError(error: PlaybackException) {
                        hideLoadingForCurrentItem()
                    }
                })
            }
    }
    private fun hideLoadingForCurrentItem() {
        val pos = viewPager.currentItem
        val recycler = viewPager.getChildAt(0) as? RecyclerView ?: return
        val vh = recycler.findViewHolderForAdapterPosition(pos) ?: return
        val loading = vh.itemView.findViewById<ProgressBar>(R.id.loading)
        loading?.visibility = View.GONE
    }

    private val pageCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            playAt(position)
        }
    }

    private fun playAt(pos: Int) {
        if (pos !in items.indices) return

        val recycler = viewPager.getChildAt(0) as? RecyclerView ?: return
        val vh = recycler.findViewHolderForAdapterPosition(pos) ?: return
        val container = vh.itemView.findViewById<ViewGroup>(R.id.playerContainer)
        val model = items[pos]
        val loading = vh.itemView.findViewById<ProgressBar>(R.id.loading)
        loading?.visibility = View.VISIBLE

        currentViewCountedVideoId = model.id

        if (sharedPlayerView?.parent != null) {
            (sharedPlayerView?.parent as ViewGroup).removeView(sharedPlayerView)
        }
        container.addView(sharedPlayerView)
        sharedPlayerView?.player = player
        player?.apply {
            stop()
            clearMediaItems()
            setMediaSource(createMediaSource(model.videoUrl))
            prepare()
            playWhenReady = true
        }
    }

    private fun handleInteraction(interaction: ShortsAdapter.Interaction) {
        val pos = interaction.position
        val model = items.getOrNull(pos) ?: return

        when (interaction.type) {
            ShortsAdapter.Interaction.Type.LIKE -> {
                if (!isLoggedIn()) {
                    askLogin(pos)
                    return
                }

                val uid = FirebaseAuth.getInstance().uid ?: return
                val item = items[pos]
                val holder = interaction.holder

                val wasLiked = item.liked

                item.liked = !wasLiked
                item.likes += if (item.liked) 1 else -1
                holder.tvLikes.text = formatCount(item.likes)
                holder.setLiked(item.liked)

                toggleLike(item.id, uid)
            }

            ShortsAdapter.Interaction.Type.COMMENT -> {
                if (!isLoggedIn()) {
                    askLogin(pos)
                    return
                }
                val sheet = CommentsBottomSheet.newInstance(model.id)
                sheet.setOnCommentAddedListener {
                    val item = items[pos]
                    item.comments += 1
                    val recycler = viewPager.getChildAt(0) as? RecyclerView
                    val vh = recycler?.findViewHolderForAdapterPosition(pos)
                    val tvComments = vh?.itemView?.findViewById<TextView>(R.id.tvComments)
                    tvComments?.text = formatCount(item.comments)
                }
                sheet.show(parentFragmentManager, "comments_sheet")
            }

            ShortsAdapter.Interaction.Type.SHARE -> {}

            ShortsAdapter.Interaction.Type.CLICK -> {}
        }
    }

    private fun isLoggedIn(): Boolean {
        return FirebaseAuth.getInstance().currentUser != null
    }

    private fun askLogin(position: Int) {
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Login Required")
            .setMessage("You need to login to like or comment on a video.")
            .setPositiveButton("Login / Signup") { _, _ ->
                val intent = Intent(requireContext(), SignupActivity::class.java)
                intent.putExtra("shorts_return_position", position)
                signupLauncher.launch(intent)
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    fun toggleLike(
        videoId: String,
        userId: String,
        onComplete: ((isLiked: Boolean) -> Unit)? = null
    ) {
        val db = FirebaseFirestore.getInstance()
        val videoRef = db.collection("videos").document(videoId)
        val likeDocRef = videoRef.collection("likedBy").document(userId)

        likeDocRef.get()
            .addOnSuccessListener { snap ->
                val currentlyLiked = snap.exists()
                if (currentlyLiked) {
                    // remove like
                    likeDocRef.delete()
                        .addOnSuccessListener {
                            // decrement likes on video doc
                            videoRef.update("likes", FieldValue.increment(-1))
                                .addOnSuccessListener {
                                    onComplete?.invoke(false)
                                }
                                .addOnFailureListener { e ->
                                    e.printStackTrace()
                                    onComplete?.invoke(false)
                                }
                        }
                        .addOnFailureListener { e ->
                            e.printStackTrace()
                            onComplete?.invoke(currentlyLiked)
                        }
                } else {
                    // add like
                    val payload = mapOf("likedAt" to System.currentTimeMillis())
                    likeDocRef.set(payload)
                        .addOnSuccessListener {
                            videoRef.update("likes", FieldValue.increment(1))
                                .addOnSuccessListener {
                                    onComplete?.invoke(true)
                                }
                                .addOnFailureListener { e ->
                                    e.printStackTrace()
                                    onComplete?.invoke(true)
                                }
                        }
                        .addOnFailureListener { e ->
                            e.printStackTrace()
                            onComplete?.invoke(false)
                        }
                }
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                onComplete?.invoke(false)
            }
    }

    override fun onPause() {
        super.onPause()
        player?.playWhenReady = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        sharedPlayerView?.player = null
        (sharedPlayerView?.parent as? ViewGroup)?.removeView(sharedPlayerView)
        sharedPlayerView = null
        player?.release()
        player = null
    }

    private fun loadVideosFromFirestore() {
        FirebaseFirestore.getInstance()
            .collection("videos")
            .orderBy("uploadedAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snap ->
                items.clear()
                snap.documents.forEach { doc ->
                    val model = ShortsModel(
                        id = doc.id,
                        videoUrl = doc.getString("videoUrl") ?: return@forEach,
                        title = doc.getString("title") ?: "",
                        uploaderId = doc.getString("uploaderId") ?: "",
                        uploaderName = doc.getString("uploaderName") ?: "",
                        likes = doc.getLong("likes") ?: 0L,
                        comments = doc.getLong("commentsCount") ?: 0L,
                        views = doc.getLong("views") ?: 0L,
                        uploadedAt = doc.getTimestamp("uploadedAt")?.toDate()?.time ?: 0L
                    )
                    items.add(model)
                }
                adapter.notifyDataSetChanged()
                syncLikedStates()
                viewPager.post {
                    if (items.isNotEmpty()) {
                        playAt(0)
                    }
                }
            }
    }

    private fun syncLikedStates() {
        val uid = FirebaseAuth.getInstance().uid ?: return
        val db = FirebaseFirestore.getInstance()

        items.forEachIndexed { index, item ->
            db.collection("videos")
                .document(item.id)
                .collection("likedBy")
                .document(uid)
                .get()
                .addOnSuccessListener { doc ->
                    item.liked = doc.exists()
                    val recycler = viewPager.getChildAt(0) as? RecyclerView ?: return@addOnSuccessListener
                    val vh = recycler.findViewHolderForAdapterPosition(index) as? ShortsAdapter.VH?: return@addOnSuccessListener
                    vh.setLiked(item.liked)
                }
        }
    }

    fun addViewOnce(videoId: String, onViewAdded: (() -> Unit)? = null) {
        if (viewedVideosSession.contains(videoId)) return
        viewedVideosSession.add(videoId)

        val db = FirebaseFirestore.getInstance()
        val videoRef = db.collection("videos").document(videoId)

        val uid = FirebaseAuth.getInstance().uid

        if (uid == null) {
            videoRef.update("views", FieldValue.increment(1))
                .addOnSuccessListener {
                    onViewAdded?.invoke()
                }
            return
        }

        val viewRef = videoRef.collection("viewedBy").document(uid)
        viewRef.get().addOnSuccessListener { snap ->
            if (!snap.exists()) {
                db.runTransaction { txn ->
                    txn.set(viewRef, mapOf("viewedAt" to FieldValue.serverTimestamp()))
                    txn.update(videoRef, "views", FieldValue.increment(1))
                }.addOnSuccessListener {
                    onViewAdded?.invoke()
                }
            }
        }
    }

    fun formatCount(count: Long): String {
        return when {
            count < 1_000 -> count.toString()
            count < 1_000_000 -> {
                val v = count / 1_000.0
                String.format("%.1fk", v).replace(".0k", "k")
            }
            count < 1_000_000_000 -> {
                val v = count / 1_000_000.0
                String.format("%.1fM", v).replace(".0M", "M")
            }
            else -> {
                val v = count / 1_000_000_000.0
                String.format("%.1fB", v).replace(".0B", "B")
            }
        }
    }

    private fun updateViewsUIForCurrentItem(newCount: Long) {
        val pos = viewPager.currentItem
        val recycler = viewPager.getChildAt(0) as? RecyclerView ?: return
        val vh = recycler.findViewHolderForAdapterPosition(pos) ?: return
        val tvViews = vh.itemView.findViewById<TextView>(R.id.tvViews)
        tvViews.text = "${formatCount(newCount)} views"
    }
}