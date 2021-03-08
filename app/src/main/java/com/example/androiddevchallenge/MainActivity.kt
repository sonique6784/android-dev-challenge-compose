/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.androiddevchallenge

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.androiddevchallenge.ui.theme.MyTheme
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.*
import kotlin.math.sqrt

class MainActivity : AppCompatActivity() {

    private val counterViewModel by viewModels<CounterViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyTheme {
                MyApp(counterViewModel)
            }
        }
    }
}

class Clock(
    var hour: String = "00",
    var minute: String = "00",
    var second: String = "00",
    var timeElapsed: Int = 0,
    var totalTime: Int = 0
)

class ButtonState(var start: Boolean = true, var stop: Boolean = false)

class CounterViewModel : ViewModel() {

    // LiveData holds state which is observed by the UI
    // (state flows down from ViewModel)
    private val _clock = MutableLiveData<Clock>()
    val clock: LiveData<Clock> = _clock

    private val _buttonState = MutableLiveData<ButtonState>()
    val buttonState: LiveData<ButtonState> = _buttonState

    private var countDownLength = 0
    private var countDownStatus = 0

    private var tickerChannel = ticker(delayMillis = 1_000, initialDelayMillis = 0)
    private var job: Job? = null

    private var jobIncrease: Job? = null
    private var jobDecrease: Job? = null


    private var start = 0L

    fun startIncrease() {
        jobIncrease?.cancel()
        jobDecrease?.cancel()

        start = Date().time

        jobIncrease = viewModelScope.launch {
            while (start != 0L) {


                val now = Date().time
                val duration = now - start

                val waitMilis = when {
                    duration < 2000 -> 300L
                    duration < 4000 -> 150L
                    duration < 6000 -> 100L
                    else -> 50L
                }

                val increment = when {
                    duration < 2000 -> 1
                    duration < 3000 -> 2
                    duration < 6000 -> 5
                    duration < 8000 -> 15
                    else -> 60
                }

                Log.d(
                    "Duration",
                    "start: $start, duration: $duration, time: $waitMilis, inc: $increment"
                )

                countDownLength += increment
                updateClock()
                delay(waitMilis)

            }
        }
    }

    fun startDecrease() {
        jobIncrease?.cancel()
        jobDecrease?.cancel()
        start = Date().time

        jobDecrease = viewModelScope.launch {
            while (start != 0L) {

                val now = Date().time
                val duration = now - start

                val waitMillis = when {
                    duration < 2000 -> 300L
                    duration < 4000 -> 150L
                    duration < 6000 -> 100L
                    else -> 50L
                }

                val increment = when {
                    duration < 2000 -> 1
                    duration < 3000 -> 2
                    duration < 6000 -> 5
                    duration < 8000 -> 15
                    else -> 60
                }

                Log.d(
                    "Duration",
                    "start: $start, duration: $duration, time: $waitMillis, inc: $increment"
                )

                countDownLength -= increment
                if (countDownLength <= 0) {
                    start = 0
                    countDownLength = 0
                }
                updateClock()
                delay(waitMillis)

            }
        }
    }

    fun stopIncrease() {
        start = 0
        jobIncrease?.cancel()
    }

    fun stopDecrease() {
        start = 0
        jobDecrease?.cancel()
    }

    private fun updateClock() {
        val timeLeft = countDownLength - countDownStatus
        val hour = timeLeft / (60 * 60) % 60
        val minute = (timeLeft / 60) % 60
        val second = timeLeft - (minute * 60) - (hour * 60 * 60)

        _clock.postValue(
            Clock(
                hour.toString().padStart(2, '0'),
                minute.toString().padStart(2, '0'),
                second.toString().padStart(2, '0'),
                countDownStatus,
                countDownLength
            )
        )
    }

    @SuppressLint("NewApi")
    fun startTimer() {
        _buttonState.postValue(ButtonState(false, true))
        job?.cancel()

        job = viewModelScope.launch {
            for (event in tickerChannel) {
                if (countDownStatus < countDownLength) {
                    // the 'event' variable is of type Unit, so we don't really care about it
                    val currentTime = LocalDateTime.now()
                    println(currentTime)

                    updateClock()

                    countDownStatus++
                } else {
                    stopTimer()
                }
            }
        }
    }

    fun stopTimer() {
        _buttonState.postValue(ButtonState(start = true, stop = false))
        job?.cancel()
        countDownStatus = 0
        countDownLength = 0
        _clock.postValue(Clock())

    }
}

// Start building your app here!
@Composable
fun MyApp(counterViewModel: CounterViewModel = CounterViewModel()) {

    val clock: Clock by counterViewModel.clock.observeAsState(Clock("00", "00", "00"))

    val buttons: ButtonState by counterViewModel.buttonState.observeAsState(initial = ButtonState())

    Surface(color = MaterialTheme.colors.background) {
        Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp)) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .wrapContentSize(Alignment.Center)
                    .padding(start = 20.dp, end = 20.dp, top = 40.dp)
            ) {
                numberInCircle(clock.hour)
                numberInCircle(clock.minute)
                numberInCircle(clock.second)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentSize(Alignment.Center)
                    .padding(top = 20.dp, bottom = 20.dp)
                    .height(200.dp)

            ) {

                val activeBorder = try {
                    Log.e(
                        "Clock",
                        "${clock.totalTime}, ${clock.timeElapsed}, ${(clock.totalTime / 80f)}"
                    )
                    if (clock.totalTime > 0) {
                        (clock.timeElapsed / (clock.totalTime / 80f)).toInt()
                    } else {
                        0
                    }
                } catch (e: Exception) {
                    0
                }

                CanvasDrawOctogon(activeBorder)

            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentSize(Alignment.Center)
                    .padding(bottom = 20.dp)
            ) {
                numberInCircle("-",
                    modifier = Modifier
                        .pointerInteropFilter {
                            when (it.action) {
                                MotionEvent.ACTION_DOWN -> {
                                    Log.d("Touch", "Down: ${it.downTime}, ${it.eventTime}")
                                    counterViewModel.startDecrease()
                                }
                                MotionEvent.ACTION_UP -> {
                                    Log.d("Touch", "Length: ${it.eventTime - it.downTime}")
                                    counterViewModel.stopDecrease()
                                }
                                else -> false
                            }
                            true
                        }
                        .padding(end = 10.dp),
                    MaterialTheme.colors.secondary,
                    MaterialTheme.colors.onSecondary)

                numberInCircle("+",
                    modifier = Modifier
                        .pointerInteropFilter {
                            when (it.action) {
                                MotionEvent.ACTION_DOWN -> {
                                    Log.d("Touch", "Down: ${it.downTime}, ${it.eventTime}")
                                    counterViewModel.startIncrease()
                                }
                                MotionEvent.ACTION_MOVE -> {

                                }
                                MotionEvent.ACTION_UP -> {
                                    Log.d("Touch", "Length: ${it.eventTime - it.downTime}")
                                    counterViewModel.stopIncrease()
                                }
                                else -> false
                            }
                            true
                        }
                        .padding(start = 10.dp),
                    MaterialTheme.colors.secondary,
                    MaterialTheme.colors.onSecondary)
            }

            Button(
                onClick = {
                    counterViewModel.startTimer()
                }, modifier = Modifier
                    .fillMaxWidth(),
                enabled = buttons.start
            ) {
                Text("Start")
            }

            Button(
                onClick = {
                    counterViewModel.stopTimer()
                }, modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                enabled = buttons.stop
            ) {
                Text("Stop")
            }
        }
    }
}

@Composable
fun numberInCircle(
    number: String,
    modifier: Modifier = Modifier,
    bgColor: Color = MaterialTheme.colors.primary,
    fgColor: Color = MaterialTheme.colors.onPrimary
) {
    Column(
        modifier = modifier
            //.wrapContentSize(Alignment.Center)
            .size(100.dp)
            .clip(CircleShape),
//                horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor),

        ) {
            Text(
                text = number,
                modifier = Modifier.fillMaxSize().padding(top = 23.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.h4,
                color = fgColor,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CanvasDrawOctogon(border: Int = 0) {
    val hexlength = 200f
    val diagonalmove = sqrt((hexlength * hexlength) / 2)
    val xstart = diagonalmove
    val ystart = 0f
    val strokeWidth = 15f
    val activeBorderColor = MaterialTheme.colors.primary
    val normalBorderColor = MaterialTheme.colors.secondary

    Canvas(modifier = Modifier.width(175.dp)) {

        val activeBorder = when {
            border / 70 > 0 -> 8
            border / 60 > 0 -> 7
            border / 50 > 0 -> 6
            border / 40 > 0 -> 5
            border / 30 > 0 -> 4
            border / 20 > 0 -> 3
            border / 10 > 0 -> 2
            else -> 1
        }

        var color = if (activeBorder == 1) {
            activeBorderColor
        } else {
            normalBorderColor
        }
        // _
        drawLine(
            color, Offset(xstart, ystart),
            Offset(xstart + hexlength, ystart), strokeWidth = strokeWidth
        )


        color = if (activeBorder == 2) {
            activeBorderColor
        } else {
            normalBorderColor
        }
        // \ diag
        drawLine(
            color, Offset(xstart + hexlength, ystart),
            Offset(xstart + hexlength + diagonalmove, ystart + diagonalmove), strokeWidth = strokeWidth
        )

        color = if (activeBorder == 3) {
            activeBorderColor
        } else {
            normalBorderColor
        }
        // |
        drawLine(
            color,
            Offset(xstart + hexlength + diagonalmove, ystart + diagonalmove),
            Offset(xstart + hexlength + diagonalmove, ystart + hexlength + diagonalmove),
            strokeWidth = strokeWidth
        )

        color = if (activeBorder == 4) {
            activeBorderColor
        } else {
            normalBorderColor
        }
        // / diag
        drawLine(
            color, Offset(xstart + hexlength + diagonalmove, ystart + hexlength + diagonalmove),
            Offset(xstart + hexlength, ystart + hexlength + 2 * diagonalmove), strokeWidth = strokeWidth
        )

        color = if (activeBorder == 5) {
            activeBorderColor
        } else {
            normalBorderColor
        }
        // _
        drawLine(
            color, Offset(xstart + hexlength, ystart + hexlength + 2 * diagonalmove),
            Offset(xstart, ystart + hexlength + 2 * diagonalmove), strokeWidth = strokeWidth
        )

        color = if (activeBorder == 6) {
            activeBorderColor
        } else {
            normalBorderColor
        }
        // \ diag
        drawLine(
            color, Offset(xstart, ystart + hexlength + 2 * diagonalmove),
            Offset(xstart - diagonalmove, ystart + hexlength + diagonalmove), strokeWidth = strokeWidth
        )

        color = if (activeBorder == 7) {
            activeBorderColor
        } else {
            normalBorderColor
        }
        // |
        drawLine(
            color, Offset(xstart - diagonalmove, ystart + hexlength + diagonalmove),
            Offset(xstart - diagonalmove, ystart + diagonalmove), strokeWidth = strokeWidth
        )


        color = if (activeBorder == 8) {
            activeBorderColor
        } else {
            normalBorderColor
        }
        // /
        drawLine(
            color, Offset(xstart - diagonalmove, ystart + diagonalmove),
            Offset(xstart, ystart), strokeWidth = strokeWidth
        )

    }
}

@Preview("Light Theme", widthDp = 360, heightDp = 640)
@Composable
fun LightPreview() {
    MyTheme {
        MyApp()
    }
}

@Preview("Dark Theme", widthDp = 360, heightDp = 640)
@Composable
fun DarkPreview() {
    MyTheme(darkTheme = true) {
        MyApp()
    }
}

