package com.example.kotlin4player

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.media.AudioAttributes
import android.media.SoundPool
// import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

import java.util.Locale

/**
 * Uses a single CountDownTimer instance to drive the currently active player.
 * CountDownTimer is available since API 1 and runs callbacks on the main thread,
 * which keeps updates safe for UI operations on API 22. [web:6]
 * Only one instance is started at a time to avoid multiple timers running.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var playerZones: List<View>
    private lateinit var timeViews: List<TextView>
    private lateinit var namesViews: List<TextView>

    private lateinit var namesInput: List<EditText>
    private lateinit var configButton: Button
    private lateinit var abortButton: Button
    private lateinit var  mainMenuButton: Button

    private lateinit var statusText: TextView

    private var playerTimesMillis = LongArray(4) { DEFAULT_TIME_MINUTES * 60_000L }
    private var currentPlayerIndex = -1

    private var isGamePaused = false
    private var isGameRunning = false
    private var isGameFinished = false

    private var activeTimer: CountDownTimer? = null
    private var activePlayerStartRemaining: Long = 0L
    private var activePlayerStartRealtime: Long = 0L

    // Colors chosen per player (resource IDs)
    private var playerColors: IntArray = IntArray(4)

    // SoundPool for short sounds (tap and time_out) [web:7][web:16]
    private var soundPool: SoundPool? = null
    private var tapSoundId: Int = 0
    private var timeoutSoundId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        initSoundPool()
        initDefaultColors()
        initPlayerZoneClicks()

        configButton.setOnClickListener {
            if (!isGameRunning && !isGameFinished) {
                showPreGameDialog()
            }

        }
        abortButton.setOnClickListener {
            finishAndRemoveTask()
        }

        mainMenuButton.setOnClickListener {
            showPopupMenu()
        }

        updateAllTimeTexts()
    }

    private fun initViews() {
        val p1Zone = findViewById<View>(R.id.player1Zone)
        val p2Zone = findViewById<View>(R.id.player2Zone)
        val p3Zone = findViewById<View>(R.id.player3Zone)
        val p4Zone = findViewById<View>(R.id.player4Zone)

        val p1Time = findViewById<TextView>(R.id.player1Time)
        val p2Time = findViewById<TextView>(R.id.player2Time)
        val p3Time = findViewById<TextView>(R.id.player3Time)
        val p4Time = findViewById<TextView>(R.id.player4Time)

        val p1Name = findViewById<TextView>(R.id.player1Name)
        val p2Name = findViewById<TextView>(R.id.player2Name)
        val p3Name = findViewById<TextView>(R.id.player3Name)
        val p4Name = findViewById<TextView>(R.id.player4Name)

        playerZones = listOf(p1Zone, p2Zone, p3Zone, p4Zone)
        timeViews = listOf(p1Time, p2Time, p3Time, p4Time)
        namesViews = listOf(p1Name, p2Name, p3Name, p4Name)

        configButton = findViewById(R.id.configButton)
        abortButton = findViewById(R.id.abortButton)
        mainMenuButton = findViewById(R.id.mainMenuButton)

        statusText = findViewById(R.id.statusText)
    }


    private fun initSoundPool() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(audioAttributes)
            .build()

        tapSoundId = soundPool?.load(this, R.raw.tap, 1) ?: 0
        timeoutSoundId = soundPool?.load(this, R.raw.time_out, 1) ?: 0
    }



    private fun initDefaultColors() {
        playerColors[0] = R.color.player1_default
        playerColors[1] = R.color.player2_default
        playerColors[2] = R.color.player3_default
        playerColors[3] = R.color.player4_default

        for (i in 0..3) {
            playerZones[i].setBackgroundColor(
                ContextCompat.getColor(this, playerColors[i])
            )
        }
    }

    private fun initPlayerZoneClicks() {
        playerZones.forEachIndexed { index, view ->
            view.setOnClickListener {
                onPlayerZoneClicked(index)
            }
        }
    }

    private fun onPlayerZoneClicked(index: Int) {
        if (!isGameRunning || isGameFinished) {
            return
        }

        if (index != currentPlayerIndex) {
            // Ignore taps on non-active players
            return
        } else {
            playTapSound()
            if (isGamePaused) {
                onResume()
            } else {
                advanceToNextPlayer()
            }
        }
    }

    private fun playTapSound() {
        if (tapSoundId != 0) {
            soundPool?.play(tapSoundId, 1f, 1f, 0, 0, 1f)
        }
    }

    private fun playTimeoutSound() {
        if (timeoutSoundId != 0) {
            soundPool?.play(timeoutSoundId, 1f, 1f, 1, 0, 1f)
        }
    }

    private fun showPopupMenu() {
        val popupMenuView = LayoutInflater.from(this).inflate(R.layout.popup_menu, null, false)

        val popupMenu = AlertDialog.Builder(this)
            .setView(popupMenuView)
            .setCancelable(false)
            .create()

        // menuGoBackButton.visibility=View.VISIBLE

        popupMenuView.findViewById<Button>(R.id.menuGoBackButton).setOnClickListener {
            popupMenu.dismiss()
        }

        popupMenuView.findViewById<Button>(R.id.menuQuitButton).setOnClickListener {
            popupMenu.dismiss()
            finishAndRemoveTask()
        }

        popupMenuView.findViewById<Button>(R.id.menuResetButton).setOnClickListener {
            popupMenu.dismiss()
            if (isGameRunning || isGameFinished) {
                showPreGameDialog()
            }
        }

        popupMenuView.findViewById<Button>(R.id.menuPauseButton).setOnClickListener {
            popupMenu.dismiss()
            onPause()

        }
        popupMenu.show()

        // Set the window background to transparent
        popupMenu.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // 1. Get the window instance
        val window = popupMenu.window

        if (window != null) {
            // 2. Set the Gravity to Bottom and Right
            window.setGravity(Gravity.BOTTOM or Gravity.END)

            // 3. Optional: Clear the "Dim" background if you want to see the UI behind it
            // window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)

            // 4. Adjust layout parameters for dimensions
            val params = window.attributes
            params.width = WindowManager.LayoutParams.WRAP_CONTENT
            params.height = WindowManager.LayoutParams.WRAP_CONTENT

            // 5. Add margins/offsets from the edge
            params.x = 0 // Horizontal offset from right
            params.y = 0 // Vertical offset from bottom
            // params.x = 20 // Horizontal offset from right
            // params.y = 20 // Vertical offset from bottom

            window.attributes = params
        }
    }


    private fun showPreGameDialog() {
        val dialogView = LayoutInflater.from(this)
            .inflate(R.layout.dialog_pregame_config, null, false)

        val timeInput = dialogView.findViewById<EditText>(R.id.timeMinutesInput)

        val p1Group = dialogView.findViewById<RadioGroup>(R.id.player1ColorGroup)
        val p2Group = dialogView.findViewById<RadioGroup>(R.id.player2ColorGroup)
        val p3Group = dialogView.findViewById<RadioGroup>(R.id.player3ColorGroup)
        val p4Group = dialogView.findViewById<RadioGroup>(R.id.player4ColorGroup)

        val p1NameInput = dialogView.findViewById<EditText>(R.id.player1NameInput)
        val p2NameInput = dialogView.findViewById<EditText>(R.id.player2NameInput)
        val p3NameInput = dialogView.findViewById<EditText>(R.id.player3NameInput)
        val p4NameInput = dialogView.findViewById<EditText>(R.id.player4NameInput)

        namesInput = listOf(p1NameInput, p2NameInput, p3NameInput, p4NameInput)

        // Preselect first color for all players to avoid invalid state
        p1Group.check(R.id.p1Color1)
        p2Group.check(R.id.p2Color2)
        p3Group.check(R.id.p3Color3)
        p4Group.check(R.id.p4Color4)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialogView.findViewById<Button>(R.id.cancelButton).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.startButton).setOnClickListener {
            val minutesText = timeInput.text.toString().trim()
            val minutes = minutesText.toLongOrNull() ?: DEFAULT_TIME_MINUTES
            val millis = minutes * 60_000L

            for (i in 0..3) {
                playerTimesMillis[i] = millis
            }
            updateAllTimeTexts()

            playerColors[0] = mapCheckedIdToColorRes(p1Group.checkedRadioButtonId)
            playerColors[1] = mapCheckedIdToColorRes(p2Group.checkedRadioButtonId)
            playerColors[2] = mapCheckedIdToColorRes(p3Group.checkedRadioButtonId)
            playerColors[3] = mapCheckedIdToColorRes(p4Group.checkedRadioButtonId)

            updatePlayerNames()

            applyPlayerColors()

            // select start player

            val selectedId = dialogView.findViewById<RadioGroup>(R.id.startPlayerRadioGroup).checkedRadioButtonId

            // Assign index based on the ID (0-indexed)
            currentPlayerIndex = when (selectedId) {
                R.id.radioPlayer1 -> 0
                R.id.radioPlayer2 -> 1
                R.id.radioPlayer3 -> 2
                R.id.radioPlayer4 -> 3
                else -> 0
            }

            dialog.dismiss()

            configButton.visibility=View.GONE
            mainMenuButton.visibility=View.VISIBLE

            startGame()
        }
        dialog.show()
    }


    private fun mapCheckedIdToColorRes(checkedId: Int): Int {
        return when (checkedId) {
            R.id.p1Color1, R.id.p2Color1, R.id.p3Color1, R.id.p4Color1 -> R.color.color_f58027
            R.id.p1Color2, R.id.p2Color2, R.id.p3Color2, R.id.p4Color2 -> R.color.color_f5d327
            R.id.p1Color3, R.id.p2Color3, R.id.p3Color3, R.id.p4Color3 -> R.color.color_33ad05
            R.id.p1Color4, R.id.p2Color4, R.id.p3Color4, R.id.p4Color4 -> R.color.color_1c7aad
            R.id.p1Color5, R.id.p2Color5, R.id.p3Color5, R.id.p4Color5 -> R.color.color_7026c9
            R.id.p1Color6, R.id.p2Color6, R.id.p3Color6, R.id.p4Color6 -> R.color.color_d60d1e
            else -> R.color.color_f58027
        }
    }

    private fun applyPlayerColors() {
        for (i in 0..3) {
            playerZones[i].setBackgroundColor(
                ContextCompat.getColor(this, playerColors[i])
            )
            playerZones[i].background.alpha = 255
        }
    }

    private fun updatePlayerNames() {
        // Update player names. Default is no name.
        for (i in 0..3) {
            namesViews[i].text = namesInput[i].text
        }
    }

    //@SuppressLint("SetTextI18n")
    private fun startGame() {
        val gameRunningMsg = getString(R.string.game_running_message)
        isGameFinished = false
        isGameRunning = true
        configButton.visibility = View.GONE
        configButton.isEnabled = false
        abortButton.visibility = View.GONE
        abortButton.isEnabled = false

        statusText.text = gameRunningMsg

        startTimerForCurrentPlayer()
        highlightActivePlayer()
    }

    private fun startTimerForCurrentPlayer() {
        cancelActiveTimer()

        val remaining = playerTimesMillis[currentPlayerIndex]
        if (remaining <= 0L) {
            onPlayerTimeOut(currentPlayerIndex)
            return
        }

        activePlayerStartRemaining = remaining
        activePlayerStartRealtime = System.currentTimeMillis()

        activeTimer = object : CountDownTimer(remaining, 100L) {
            override fun onTick(millisUntilFinished: Long) {
                playerTimesMillis[currentPlayerIndex] = millisUntilFinished
                updateTimeText(currentPlayerIndex)
            }

            override fun onFinish() {
                playerTimesMillis[currentPlayerIndex] = 0L
                updateTimeText(currentPlayerIndex)
                onPlayerTimeOut(currentPlayerIndex)
            }
        }.start()
    }

    private fun cancelActiveTimer() {
        activeTimer?.cancel()
        activeTimer = null
    }

    private fun advanceToNextPlayer() {
        cancelActiveTimer()

        // Update remaining based on elapsed real time to avoid skips on resume
        val elapsed = System.currentTimeMillis() - activePlayerStartRealtime
        val newRemaining = (activePlayerStartRemaining - elapsed).coerceAtLeast(0L)
        playerTimesMillis[currentPlayerIndex] = newRemaining
        updateTimeText(currentPlayerIndex)

        if (newRemaining <= 0L) {
            onPlayerTimeOut(currentPlayerIndex)
            return
        }

        currentPlayerIndex = (currentPlayerIndex + 1) % 4
        highlightActivePlayer()
        startTimerForCurrentPlayer()
    }

    private fun onPlayerTimeOut(playerIndex: Int) {
        val playerId: Int = playerIndex + 1
        val playerTimeoutMsg = getString(R.string.timed_out_message, playerId)
        isGameFinished = true
        isGameRunning = false
        playTimeoutSound()
        cancelActiveTimer()
        configButton.visibility = View.GONE
        configButton.isEnabled = false
        abortButton.visibility = View.GONE
        abortButton.isEnabled = false


        // Visually indicate timeout: dim others and keep timed-out as-is, plus border effect via alpha tweak
        for (i in 0..3) {
            val zone = playerZones[i]
            if (i == playerIndex) {
                zone.alpha = 1.0f
            } else {
                zone.alpha = 0.3f
            }
        }

        statusText.text = playerTimeoutMsg
    }

    private fun updateAllTimeTexts() {
        for (i in 0..3) {
            updateTimeText(i)
        }
    }

    private fun updateTimeText(playerIndex: Int) {
        val millis = playerTimesMillis[playerIndex]
        val totalSeconds = millis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val formatted = "%2d:%02d".format(Locale.getDefault(), minutes, seconds)
        timeViews[playerIndex].text = formatted
    }

    private fun highlightActivePlayer() {
        if (currentPlayerIndex !in 0..3) return

        for (i in 0..3) {
            val zone = playerZones[i]
            zone.alpha = if (i == currentPlayerIndex) 1.0f else 0.6f
        }
    }

    override fun onPause() {
        super.onPause()
        if (isGameRunning && !isGameFinished) {
            // Pause timer and store remaining accurately
            val elapsed = System.currentTimeMillis() - activePlayerStartRealtime
            val newRemaining = (activePlayerStartRemaining - elapsed).coerceAtLeast(0L)
            val gamePausedMsg = getString(R.string.game_paused_message)
            playerTimesMillis[currentPlayerIndex] = newRemaining
            cancelActiveTimer()
            updateTimeText(currentPlayerIndex)
            isGamePaused = true
            statusText.text = gamePausedMsg
        }
    }

    override fun onResume() {
        super.onResume()
        val gameRunningMsg = getString(R.string.game_running_message)
        isGamePaused = false
        if (isGameRunning && !isGameFinished && currentPlayerIndex in 0..3) {
            activePlayerStartRemaining = playerTimesMillis[currentPlayerIndex]
            activePlayerStartRealtime = System.currentTimeMillis()
            startTimerForCurrentPlayer()
            statusText.text = gameRunningMsg
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelActiveTimer()
        soundPool?.release()
        soundPool = null
    }

    companion object {
        private const val DEFAULT_TIME_MINUTES = 10L
    }
}
