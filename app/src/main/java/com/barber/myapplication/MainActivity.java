package com.barber.myapplication;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private CitaAdapter adapter;
    private final List<Cita> citaList = new ArrayList<>();
    private DatabaseReference databaseReference;

    private static final String MQTT_BROKER_URL = "tcp://broker.emqx.io:1883";  // Puedes cambiarlo por tu broker MQTT
    private static final String MQTT_TOPIC = "citas/agregadas"; // Tópico al que publicaremos
    private MqttClient mqttClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Iniciar conexión MQTT
        try {
            mqttClient = new MqttClient(MQTT_BROKER_URL, MqttClient.generateClientId(), null);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            mqttClient.connect(options);
        } catch (MqttException e) {
            Log.e("MQTT", "Error al conectar al broker MQTT", e);
        }

        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CitaAdapter(citaList);
        recyclerView.setAdapter(adapter);

        // Configura el listener aquí, dentro del onCreate()
        adapter.setOnItemLongClickListener(cita -> {
            // Acción a realizar al hacer click largo (editar/eliminar)
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Acciones")
                    .setItems(new CharSequence[]{"Editar", "Eliminar"}, (dialog, which) -> {
                        if (which == 0) {
                            // Editar la cita
                            showEditDialog(cita);
                        } else if (which == 1) {
                            // Eliminar la cita
                            deleteCita(cita.getId());
                        }
                    }).show();
        });

        databaseReference = FirebaseDatabase.getInstance().getReference("citas");

        // Crear los objetos de citas
        Cita cita1 = new Cita("id1", "Juan Pérez", "03/12/2024", "10:00");
        Cita cita2 = new Cita("id2", "Ana Gómez", "04/12/2024", "11:00");
        Cita cita3 = new Cita("id3", "Carlos Rodríguez", "05/12/2024", "12:00");

        // Insertar los objetos en Firebase usando databaseReference (en lugar de database)
        databaseReference.child("id1").setValue(cita1);
        databaseReference.child("id2").setValue(cita2);
        databaseReference.child("id3").setValue(cita3);

        Button btnAdd = findViewById(R.id.btn_add);
        btnAdd.setOnClickListener(v -> showAddDialog());

        fetchCitas();
    }


    private void fetchCitas() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                citaList.clear();
                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                    Cita cita = postSnapshot.getValue(Cita.class);
                    citaList.add(cita);
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Error al cargar datos", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_add_cita, null);

        final EditText etNombre = view.findViewById(R.id.et_nombre);
        final EditText etFecha = view.findViewById(R.id.et_fecha);
        final EditText etHora = view.findViewById(R.id.et_hora);
        Button btnGuardar = view.findViewById(R.id.btn_guardar);

        Calendar calendar = Calendar.getInstance();

        etFecha.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(this, (datePicker, year, month, dayOfMonth) -> {
                String fecha = dayOfMonth + "/" + (month + 1) + "/" + year;
                etFecha.setText(fecha);
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            datePickerDialog.show();
        });

        etHora.setOnClickListener(v -> {
            TimePickerDialog timePickerDialog = new TimePickerDialog(this, (timePicker, hourOfDay, minute) -> {
                String hora = hourOfDay + ":" + (minute < 10 ? "0" + minute : minute);
                etHora.setText(hora);
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true);
            timePickerDialog.show();
        });

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(view)
                .setTitle("Agregar Cita")
                .setNegativeButton("Cancelar", null)
                .create();

        btnGuardar.setOnClickListener(v -> {
            String nombre = etNombre.getText().toString().trim();
            String fecha = etFecha.getText().toString().trim();
            String hora = etHora.getText().toString().trim();

            if (nombre.isEmpty() || fecha.isEmpty() || hora.isEmpty()) {
                Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show();
                return;
            }

            addCitaToFirebase(nombre, fecha, hora);
            dialog.dismiss();
        });

        dialog.show();
    }

    // Método para agregar la cita a Firebase y luego enviar el mensaje MQTT
    private void addCitaToFirebase(String nombre, String fecha, String hora) {
        String id = databaseReference.push().getKey(); // Genera un ID único para la cita
        Cita cita = new Cita(id, nombre, fecha, hora);

        // Guardar en Firebase
        databaseReference.child(id).setValue(cita)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Cita guardada", Toast.LENGTH_SHORT).show();
                    sendMessage("Se agregó una cita: " + nombre + " para el " + fecha + " a las " + hora);  // Enviar el mensaje MQTT
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error al guardar cita", Toast.LENGTH_SHORT).show());
    }


    private void showEditDialog(Cita cita) {
        View view = getLayoutInflater().inflate(R.layout.dialog_add_cita, null);

        final EditText etNombre = view.findViewById(R.id.et_nombre);
        final EditText etFecha = view.findViewById(R.id.et_fecha);
        final EditText etHora = view.findViewById(R.id.et_hora);
        Button btnGuardar = view.findViewById(R.id.btn_guardar);

        // Rellenar los campos con los datos de la cita
        etNombre.setText(cita.getNombre());
        etFecha.setText(cita.getFecha());
        etHora.setText(cita.getHora());

        Calendar calendar = Calendar.getInstance();

        etFecha.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(this, (datePicker, year, month, dayOfMonth) -> {
                String fecha = dayOfMonth + "/" + (month + 1) + "/" + year;
                etFecha.setText(fecha);
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            datePickerDialog.show();
        });

        etHora.setOnClickListener(v -> {
            TimePickerDialog timePickerDialog = new TimePickerDialog(this, (timePicker, hourOfDay, minute) -> {
                String hora = hourOfDay + ":" + (minute < 10 ? "0" + minute : minute);
                etHora.setText(hora);
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true);
            timePickerDialog.show();
        });

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(view)
                .setTitle("Editar Cita")
                .setNegativeButton("Cancelar", null)
                .create();

        btnGuardar.setOnClickListener(v -> {
            String nombre = etNombre.getText().toString().trim();
            String fecha = etFecha.getText().toString().trim();
            String hora = etHora.getText().toString().trim();

            if (nombre.isEmpty() || fecha.isEmpty() || hora.isEmpty()) {
                Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show();
                return;
            }

            updateCita(cita.getId(), nombre, fecha, hora);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void updateCita(String id, String nombre, String fecha, String hora) {
        Cita cita = new Cita(id, nombre, fecha, hora);

        // Actualiza la cita en Firebase
        databaseReference.child(id).setValue(cita)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Cita actualizada", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Error al actualizar cita", Toast.LENGTH_SHORT).show());
    }



    private void deleteCita(String id) {
        databaseReference.child(id).removeValue()
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Cita eliminada", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Error al eliminar cita", Toast.LENGTH_SHORT).show());
    }

    // Método para enviar el mensaje MQTT
    private void sendMessage(String message) {
        try {
            MqttMessage mqttMessage = new MqttMessage(message.getBytes());
            mqttMessage.setQos(1);  // QoS nivel 1 (entrega garantizada)
            mqttClient.publish(MQTT_TOPIC, mqttMessage);  // Publica el mensaje en el tópico
            Log.d("MQTT", "Mensaje enviado: " + message);
        } catch (MqttException e) {
            Log.e("MQTT", "Error al enviar el mensaje", e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mqttClient != null && mqttClient.isConnected()) {
            try {
                mqttClient.disconnect();
            } catch (MqttException e) {
                Log.e("MQTT", "Error al desconectar el cliente MQTT", e);
            }
        }
    }

}
