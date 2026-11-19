package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.security.MessageDigest

/**
 * Deterministic Visual UPI QR Matrix Card rendered cleanly on Canvas
 * with 3 finder patterns + SHA-256 encoded verification payload bits.
 */
@Composable
fun UpiVerificationQrMatrix(
    upiUriPayload: String,
    sizeDp: Dp = 136.dp,
    modifier: Modifier = Modifier
) {
    val matrix = remember(upiUriPayload) { buildQrGrid(upiUriPayload, 21) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(sizeDp)
            .background(Color.White, RoundedCornerShape(10.dp))
            .padding(8.dp)
    ) {
        Canvas(modifier = Modifier.size(sizeDp - 16.dp)) {
            val gridCount = matrix.size
            val cellSize = size.width / gridCount
            val darkColor = Color(0xFF0F3D28)

            for (r in 0 until gridCount) {
                for (c in 0 until gridCount) {
                    if (matrix[r][c]) {
                        drawRect(
                            color = darkColor,
                            topLeft = Offset(c * cellSize, r * cellSize),
                            size = Size(cellSize * 0.96f, cellSize * 0.96f)
                        )
                    }
                }
            }
        }
    }
}

private fun buildQrGrid(payload: String, dim: Int): Array<BooleanArray> {
    val grid = Array(dim) { BooleanArray(dim) }

    fun drawFinder(startR: Int, startC: Int) {
        for (r in 0 until 7) {
            for (c in 0 until 7) {
                val isBorder = r == 0 || r == 6 || c == 0 || c == 6
                val isCenter = r in 2..4 && c in 2..4
                grid[startR + r][startC + c] = isBorder || isCenter
            }
        }
    }

    drawFinder(0, 0)
    drawFinder(0, dim - 7)
    drawFinder(dim - 7, 0)

    val digest = MessageDigest.getInstance("SHA-256")
        .digest(payload.toByteArray(Charsets.UTF_8))

    var bitIdx = 0
    for (r in 0 until dim) {
        for (c in 0 until dim) {
            val inTopLeft = r < 8 && c < 8
            val inTopRight = r < 8 && c >= dim - 8
            val inBottomLeft = r >= dim - 8 && c < 8
            if (!inTopLeft && !inTopRight && !inBottomLeft) {
                val byteVal = digest[(bitIdx / 8) % digest.size].toInt() and 0xFF
                val bit = (byteVal shr (bitIdx % 8)) and 1
                grid[r][c] = bit == 1 || ((r + c) % 5 == 0)
                bitIdx++
            }
        }
    }
    return grid
}
