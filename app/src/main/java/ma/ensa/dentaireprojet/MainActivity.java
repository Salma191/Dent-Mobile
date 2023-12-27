package ma.ensa.dentaireprojet;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;


import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    ImageView imageView;
    FloatingActionButton button;
    private Canvas canvas;
    private Paint paint;
    private Bitmap bitmap;
    private double angleLeftLine = 0;
    private double angleRightLine = 0;
    private double convergenceAngle = 0;

    private List<Point> dentBorderPointsList = new ArrayList<>();
    private List<Point> selectedPoints = new ArrayList<>();


    private Mat edges;
    // Constante pour la distance maximale des lignes détectées du point sélectionné
    private static final double DISTANCE_THRESHOLD = 20.0; // À adapter selon les besoins



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (!OpenCVLoader.initDebug())
            Log.d("opencv", "failed salma");
        else
            Log.d("opencv", "success salma");



        Button btnShowPopup = findViewById(R.id.btnShowPopup);
        btnShowPopup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Appeler la méthode showPopupDialog pour afficher les résultats
                showPopupDialog();
            }
        });

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Gérer les événements de clic sur les éléments de la barre de navigation
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.menu_item_1:
                        Intent profileIntent1 = new Intent(MainActivity.this, ProfileActivity.class);
                        startActivity(profileIntent1);
                        return true;
                    case R.id.menu_item_2:
                        Intent profileIntent2 = new Intent(MainActivity.this, MainActivity.class);
                        startActivity(profileIntent2);
                        return true;
                    case R.id.menu_item_3:
                        Intent profileIntent3 = new Intent(MainActivity.this, LoginActivity.class);
                        startActivity(profileIntent3);
                        return true;
                }
                return false;
            }
        });

//        Button btnRefresh = findViewById(R.id.btnRefresh);
//        btnRefresh.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                // Insérez ici le code pour rafraîchir la photo
//                refreshPhoto(); // Exemple : fonction appelée pour rafraîchir l'image
//            }
//        });

        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(0xFFFF0000);
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(20); // Épaisseur du trait

        imageView = findViewById(R.id.imageView);
        button = findViewById(R.id.floatingActionButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ImagePicker.with(MainActivity.this)
                        .crop()                    //Crop image(Optional), Check Customization for more option
                        .compress(1024)            //Final image size will be less than 1 MB(Optional)
                        .maxResultSize(1080, 1080)    //Final image resolution will be less than 1080 x 1080(Optional)
                        .start();
            }
        });


    }

    private void showPopupDialog() {
        // Utilisez AlertDialog ou tout autre composant de popup que vous préférez
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Résultats");

        // Formatez les angles pour afficher trois chiffres après la virgule
        String formattedMessage = String.format("Angle Left Line: %.3f\nAngle Right Line: %.3f\nConvergence Angle: %.3f",
                angleLeftLine, angleRightLine, convergenceAngle);

        builder.setMessage(formattedMessage);

        // Ajoutez un bouton "OK" pour fermer le popup
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        // Afficher le popup
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void refreshPhoto() {
        // Si vous avez conservé l'image originale dans une variable, rechargez-la
        // Exemple : si bitmapOriginal contient l'image originale, réaffectez-la à l'ImageView
        imageView.setImageBitmap(bitmap);
        selectedPoints.clear();

        // Réinitialisez d'autres éléments si nécessaire, comme les lignes tracées, les annotations, etc.
        // Par exemple, si vous avez tracé des lignes sur l'image, effacez ces lignes ici.
    }


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null) {
            Uri uri = data.getData();
            imageView.setImageURI(uri);

            try {
                bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
                Mat rgba = new Mat();
                Utils.bitmapToMat(bitmap, rgba);

                // Convertir l'image en niveaux de gris
                Mat grayMat = new Mat();
                Imgproc.cvtColor(rgba, grayMat, Imgproc.COLOR_RGB2GRAY);

                dentBorderPointsList = new ArrayList<>();
                drawOptimalPoints(dentBorderPointsList);

                // Appliquer un flou gaussien
                Imgproc.GaussianBlur(grayMat, grayMat, new Size(5, 5), 0);

                // Detecter les bords avec l'algorithme de Canny
                edges = new Mat();
                Imgproc.Canny(grayMat, edges, 50, 150);

                // Appliquer dilation pour connecter les composants
                Imgproc.dilate(edges, edges, new Mat(), new Point(-1, -1), 2);

                // Afficher l'image avec les lignes détectées
                bitmap = matToBitmap(edges);
                imageView.setImageBitmap(bitmap);


                // Vérifier que dentBorderPointsList est initialisé et non nul
                if (dentBorderPointsList != null) {
                    // Après la détection des bords (avant la création du Canvas)
                    dentBorderPointsList = findContourPoints(edges);
                    // Configurer le Canvas pour dessiner
                    canvas = new Canvas(bitmap);
                    imageView.setOnTouchListener(new View.OnTouchListener() {
                        @Override
                        public boolean onTouch(View v, MotionEvent event) {
                            float x = event.getX();
                            float y = event.getY();

                            // Vérifier si l'action de l'événement est ACTION_DOWN
                            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                                // Convertir les coordonnées de l'événement touch en coordonnées d'image
                                Matrix inverse = new Matrix();
                                imageView.getImageMatrix().invert(inverse);
                                float[] point = new float[]{x, y};
                                inverse.mapPoints(point);

                                x = point[0];
                                y = point[1];

                                // Ajouter le point à la liste des points sélectionnés
                                Point selectedPoint = new Point(x, y);

                                // Vérifier que dentBorderPointsList est initialisé et non nul
                                if (dentBorderPointsList != null) {
                                    // Trouver le point le plus proche et optimal parmi les points sur les bords
                                    Point closestOptimalPoint = findClosestOptimalPoint(selectedPoint, dentBorderPointsList);

                                    // Dans votre méthode onTouch
                                    if (closestOptimalPoint != null) {
                                        // Vérifier si le point optimal est à l'intérieur des bords et n'a pas été déjà sélectionné
                                        if (isPointInsideContour(closestOptimalPoint, dentBorderPointsList) && !selectedPoints.contains(closestOptimalPoint)) {
                                            // Dessiner un cercle à l'emplacement du point sélectionné
                                            //canvas.drawCircle((float) selectedPoint.x, (float) selectedPoint.y, 10, paint);

                                            // Dessiner un cercle à l'emplacement du point optimal
                                            canvas.drawCircle((float) closestOptimalPoint.x, (float) closestOptimalPoint.y, 10, paint);

                                            // Mettre à jour l'image affichée
                                            imageView.invalidate();

                                            // Afficher les coordonnées dans la console Log
                                            Log.d("MyTag", "Selected Point X: " + selectedPoint.x + ", Y: " + selectedPoint.y);
                                            Log.d("MyTag", "Optimal Point X: " + closestOptimalPoint.x + ", Y: " + closestOptimalPoint.y);

                                            // Ajouter le point le plus proche et optimal à la liste des points sélectionnés
                                            selectedPoints.add(closestOptimalPoint);

                                            // Si vous avez suffisamment de points, vous pouvez appeler la détection de lignes ici
                                            if (selectedPoints.size() == 4) {
                                                performHoughLinesDetection();
                                            }
                                        }
                                    }


                                }
                            }
                            return true;
                        }
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private Bitmap matToBitmap(Mat inputMat) {
        Bitmap bitmap = Bitmap.createBitmap(inputMat.cols(), inputMat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(inputMat, bitmap);
        return bitmap;
    }

    // Dans votre méthode drawOptimalPoints, dessinez les points optimaux
    private void drawOptimalPoints(List<Point> points) {
        if (canvas != null && points != null) {
            for (Point point : points) {
                canvas.drawCircle((float) point.x, (float) point.y, 10, paint);
            }
            imageView.invalidate();
        }
    }

    private void performHoughLinesDetection() {
        try {
            if (bitmap != null && selectedPoints.size() == 4) {
                // Convertir la Bitmap en Mat
                Mat originalMat = new Mat();
                Utils.bitmapToMat(bitmap, originalMat);

                // Convertir les points en MatOfPoint2f pour la détection de lignes
                MatOfPoint2f matOfPoint2f = new MatOfPoint2f();
                matOfPoint2f.fromList(selectedPoints);

                // Appliquer la détection des lignes de Hough avec la matrice edges
                Mat lines = new Mat();
                Imgproc.HoughLines(edges, lines, 1, Math.PI / 180, 150);

                // Initialiser les variables pour les lignes gauche et droite les plus optimales
                Line bestLeftLine = null;
                Line bestRightLine = null;
                double imageCenterX = originalMat.cols() / 2.0;

                // Filtrer les lignes horizontales (lignes non verticales)
                for (int x = 0; x < lines.rows(); x++) {
                    double[] vec = lines.get(x, 0);
                    double rho = vec[0];
                    double theta = vec[1];
                    double a = Math.cos(theta);
                    double b = Math.sin(theta);
                    double x0 = a * rho;
                    double y0 = b * rho;
                    Point pt1 = new Point(Math.round(x0 + 1000 * (-b)), Math.round(y0 + 1000 * (a)));
                    Point pt2 = new Point(Math.round(x0 - 1000 * (-b)), Math.round(y0 - 1000 * (a)));

                    // Déterminer si la ligne est à gauche ou à droite en fonction du point de départ
                    boolean isLeftLine = pt1.x < imageCenterX;
                    boolean isRightLine = pt1.x >= imageCenterX;

                    // Ligne gauche
                    if (isLeftLine) {
                        if (bestLeftLine == null || pt1.x < bestLeftLine.getStartPoint().x) {
                            bestLeftLine = new Line(vec);
                        }
                    }
                    // Ligne droite
                    else if (isRightLine) {
                        if (bestRightLine == null || pt1.x > bestRightLine.getStartPoint().x) {
                            bestRightLine = new Line(vec);
                        }
                    }
                }

                // Convertir le résultat de Mat à Bitmap si nécessaire
                Bitmap resultBitmap = matToBitmap(originalMat);

                // Afficher l'image résultante dans une ImageView ou faire d'autres traitements
                imageView.setImageBitmap(resultBitmap);

                // Initialiser le Canvas après l'affichage de l'image résultante
                canvas = new Canvas(resultBitmap);

                // Dessiner les points optimaux sur les bords
                //drawOptimalPoints(dentBorderPointsList);

                // À ajouter après le dessin des lignes gauche et droite
                if (bestLeftLine != null && bestRightLine != null) {
                    // Calculer l'angle des lignes gauche et droite
                    double angleLeftLine = Math.toDegrees(Math.atan(bestLeftLine.getSlope()));
                    double angleRightLine = Math.toDegrees(Math.atan2(bestRightLine.getEndY() - bestRightLine.getStartY(),
                            bestRightLine.getEndX() - bestRightLine.getStartX()));

                    // Calculer les coordonnées des points sur les lignes gauche et droite
                    Point leftPoint1 = bestLeftLine.getStartPoint();
                    Point leftPoint2 = bestLeftLine.getEndPoint();
                    Point rightPoint1 = bestRightLine.getStartPoint();
                    Point rightPoint2 = bestRightLine.getEndPoint();

                    // Dessiner les tangentes passant par les points sélectionnés
                    drawTangents(Arrays.asList(selectedPoints.get(0), selectedPoints.get(1), selectedPoints.get(2), selectedPoints.get(3)));

                    // Calculer l'angle entre les deux tangentes
                    double angleBetweenTangents = calculateAngleBetweenLines(bestLeftLine, bestRightLine);

                    // Afficher l'angle entre les deux tangentes dans la console Log
                    Log.d("MyTag", "Angle Between Tangents: " + angleBetweenTangents);

                    // Vérifier si les tangentes convergent vers l'intérieur
                    if (angleBetweenTangents < 90) {
                        Log.d("MyTag", "Tangents converge towards the center");
                    } else {
                        Log.d("MyTag", "Tangents do not converge towards the center");
                    }

                    // Assigner les résultats aux variables de classe
                    this.angleLeftLine = Math.toDegrees(Math.atan(bestLeftLine.getSlope()));
                    this.angleRightLine = Math.toDegrees(Math.atan2(bestRightLine.getEndY() - bestRightLine.getStartY(),
                            bestRightLine.getEndX() - bestRightLine.getStartX()));
                    this.convergenceAngle = angleLeftLine + angleRightLine;


                    // Afficher les angles dans la console Log
                    Log.d("MyTag", "Angle Left Line: " + angleLeftLine);
                    Log.d("MyTag", "Angle Right Line: " + angleRightLine);
                    Log.d("MyTag", "Convergence Angle: " + (angleLeftLine + angleRightLine));

                    // Afficher les valeurs des tangentes dans la console Log
                    //Log.d("MyTag", "Tan Left Angle: " + tanLeftAngle);
                    //Log.d("MyTag", "Tan Right Angle: " + tanRightAngle);
                    Log.d("MyTag", "performHoughLinesDetection: Finished!");
                }

            } else {
                // Log ou gestion d'erreur en cas de Bitmap nulle ou de nombre de points incorrect
                Log.d("MyTag", "performHoughLinesDetection: originalBitmap is null or invalid number of points");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<Point> findContourPoints(Mat edges) {
        List<Point> contourPoints = new ArrayList<>();
        Mat hierarchy = new Mat();
        List<MatOfPoint> contours = new ArrayList<>();

        // Trouver les contours dans l'image d'arêtes
        Imgproc.findContours(edges, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        // Ajouter les points de contour à la liste
        for (MatOfPoint contour : contours) {
            contourPoints.addAll(contour.toList());
        }

        return contourPoints;
    }

    private Point findClosestOptimalPoint(Point touchPoint, List<Point> optimalPoints) {
        Point closestPoint = null;
        double minDistance = Double.MAX_VALUE;

        for (Point optimalPoint : optimalPoints) {
            double distance = calculateDistance(touchPoint, optimalPoint);

            if (distance < minDistance) {
                minDistance = distance;
                closestPoint = optimalPoint;
            }
        }

        return closestPoint;
    }

    private double calculateDistance(Point p1, Point p2) {
        if (p1 != null && p2 != null) {
            double deltaX = p1.x - p2.x;
            double deltaY = p1.y - p2.y;
            return Math.sqrt(deltaX * deltaX + deltaY * deltaY);
        }
        return Double.MAX_VALUE; // Ou une autre valeur appropriée si l'un des points est nul
    }

    private double calculateAngleBetweenLines(Line line1, Line line2) {
        double slope1 = line1.getSlope();
        double slope2 = line2.getSlope();

        // Calculer l'angle entre les deux tangentes en radians
        double angleRad = Math.atan(Math.abs((slope2 - slope1) / (1 + slope1 * slope2)));

        // Convertir l'angle en degrés
        double angleDeg = Math.toDegrees(angleRad);

        return angleDeg;
    }

    private void drawTangents(List<Point> points) {
        if (canvas != null && points != null && points.size() == 4) {
            paint.setColor(0xFF00FF00); // Changer la couleur pour les tangentes
            paint.setStrokeWidth(4); // Changer l'épaisseur du trait pour les tangentes

            // Extraire les points des bords de la dent
            Point leftPoint1 = points.get(0);
            Point leftPoint2 = points.get(1);
            Point rightPoint1 = points.get(2);
            Point rightPoint2 = points.get(3);

            // Calculer les intersections des tangentes
            Point intersection = calculateIntersection(leftPoint1, leftPoint2, rightPoint1, rightPoint2);

            // Dessiner les tangentes passant par les points sélectionnés
            if (intersection != null) {
                drawTangent(leftPoint1, intersection);
                drawTangent(leftPoint2, intersection);
                drawTangent(rightPoint1, intersection);
                drawTangent(rightPoint2, intersection);
            }

            imageView.invalidate();
        }
    }


    private static class Line {
        private final double rho;
        private final double theta;

        public Line(double[] vec) {
            this.rho = vec[0];
            this.theta = vec[1];
        }

        public Point getStartPoint() {
            double a = Math.cos(theta);
            double b = Math.sin(theta);
            double x0 = a * rho;
            double y0 = b * rho;
            return new Point(Math.round(x0 + 1000 * (-b)), Math.round(y0 + 1000 * (a)));
        }

        public Point getEndPoint() {
            double a = Math.cos(theta);
            double b = Math.sin(theta);
            double x0 = a * rho;
            double y0 = b * rho;
            return new Point(Math.round(x0 - 1000 * (-b)), Math.round(y0 - 1000 * (a)));
        }

        public double getSlope() {
            return -Math.tan(theta); // N'oubliez pas le signe négatif pour obtenir la pente correcte
        }

        public double getStartX() {
            return getStartPoint().x;
        }

        public double getStartY() {
            return getStartPoint().y;
        }

        public double getEndX() {
            return getEndPoint().x;
        }

        public double getEndY() {
            return getEndPoint().y;
        }
    }

    private boolean isPointInsideContour(Point point, List<Point> contourPoints) {
        // Créer une matrice de points de contour
        MatOfPoint2f contourMat = new MatOfPoint2f(contourPoints.toArray(new Point[0]));

        // Vérifier si le point est à l'intérieur des bords en utilisant pointPolygonTest
        double distance = Imgproc.pointPolygonTest(contourMat, point, true);

        // Si la distance est positive, le point est à l'intérieur des bords
        return distance >= 0;
    }


    private void drawTangent(Point point, Point intersection) {
        // Calculer la pente de la tangente
        double tangentSlope = (intersection.y - point.y) / (intersection.x - point.x);
        // Calculer les coordonnées des extrémités de la tangente
        double length = 150; // Changer la longueur de la tangente si nécessaire
        double xEnd = point.x + length;
        double yEnd = point.y + length * tangentSlope;
        double xStart = point.x - length;
        double yStart = point.y - length * tangentSlope;

        // Dessiner la tangente sur le Canvas
        canvas.drawLine((float) xStart, (float) yStart, (float) xEnd, (float) yEnd, paint);
    }


    private Point calculateIntersection(Point point1, Point point2, Point point3, Point point4) {
        // Calculer les coefficients des équations des droites
        double x1 = point1.x, y1 = point1.y;
        double x2 = point2.x, y2 = point2.y;
        double x3 = point3.x, y3 = point3.y;
        double x4 = point4.x, y4 = point4.y;

        double det = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4);

        // Vérifier si les droites sont parallèles (déterminant égal à zéro)
        if (Math.abs(det) == 0) {
            return null; // Les droites sont parallèles, pas d'intersection
        }

        // Calculer les coordonnées du point d'intersection
        double intersectX = ((x1 * y2 - y1 * x2) * (x3 - x4) - (x1 - x2) * (x3 * y4 - y3 * x4)) / det;
        double intersectY = ((x1 * y2 - y1 * x2) * (y3 - y4) - (y1 - y2) * (x3 * y4 - y3 * x4)) / det;

        return new Point(intersectX, intersectY);
    }


}


