package env;

// pong environment
public class Pong {
	public float ballX = 0.0f, ballY = 0.0f;
	public float ballVelX = 0.0f, ballVelY = 0.0f;

	public float paddleX = 0.0f;
	public float paddleWidth = 1.0f;

	public float screenWidth = 10.0f;
	public float screenHeight = 7.0f;

	public float paddleSpeed = 1.0f;


	//  1 : move right
	//  0 : don't move
	// -1 : move left
	public int control = 0;

	// 0 : nothing
	// 1 : hit
	// -1 : miss
	public int ballAction = 0;

	public void tick(float deltaT) {
		// move ball
		ballX = ballX + deltaT * ballVelX;
		ballY = ballY + deltaT * ballVelY;

		// check ball wall collisions

		if( ballX < 0 ) {
			ballX = 0;
			ballVelX *= -1;
		}
		else if( ballX > screenWidth ) {
			ballX = screenWidth;
			ballVelX *= -1;
		}

		if( ballY < 0 ) {
			ballY = 0;
			ballVelY *= -1;
		}

		// check ball paddle collisions

		ballAction = 0;

		if( ballY > screenHeight ) {
			boolean hitPaddle = Math.abs(ballX - paddleX) < paddleWidth / 2;
			ballAction = hitPaddle ? 1 : -1;

			if( hitPaddle ) {
				ballVelY *= -1;
			}
		}

		// move and limit paddle

		paddleX += ( (float)control * paddleSpeed * deltaT );

		if( paddleX < 0 + paddleWidth / 2 ) {
			paddleX = paddleWidth / 2;
		}
		else if( paddleX > screenWidth - paddleWidth / 2 ) {
			paddleX = screenWidth - paddleWidth / 2;
		}
	}
}
