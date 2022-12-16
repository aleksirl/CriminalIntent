package com.bignerbranch.android.criminal_intent

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer

import androidx.lifecycle.ViewModelProviders
import java.io.File
import java.util.Date

import java.util.UUID
import kotlin.math.roundToInt

private const val TAG = "CrimeFragment"
private const val ARG_CRIME_ID = "crime_id"
private const val DIALOG_DATE = "DialogDate"
private const val REQUEST_DATE = "0"
private const val DATE_FORMAT = "EEE, MMM, dd"
private const val REQUEST_CONTACT = "1"
class CrimeFragment: Fragment() {

    private lateinit var crime: Crime
    private lateinit var photoFile: File
    private lateinit var titleField: EditText
    private lateinit var dateButton: Button
    private lateinit var solvedCheckBox: CheckBox
    private lateinit var reportButton: Button
    private lateinit var suspectButton: Button
    private lateinit var photoButton: ImageButton
    private lateinit var photoView: ImageView
    private lateinit var photoUri: Uri

    private val crimeDetailViewModel: CrimeDetailViewModel by lazy {
        ViewModelProviders.of(this).get(CrimeDetailViewModel::class.java)
    }
    private val selectSuspect = registerForActivityResult(
        ActivityResultContracts.PickContact()
    ) { uri: Uri? ->
        uri?.let { pickContact(it) }
    }

    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()){
        if (it){
            updatePhotoView()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        crime = Crime()
        val crimeId: UUID = arguments?.getSerializable(ARG_CRIME_ID) as UUID
        //Log.d(TAG,"args bundle crime ID: $crimeId")
        crimeDetailViewModel.loadCrime(crimeId)

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_crime, container, false)
        titleField = view.findViewById(R.id.crime_title) as EditText
        dateButton = view.findViewById(R.id.crime_date) as Button
        solvedCheckBox = view.findViewById(R.id.crime_solved) as CheckBox
        reportButton = view.findViewById(R.id.crime_report) as Button
        suspectButton = view.findViewById(R.id.crime_suspect) as Button
        photoButton = view.findViewById(R.id.crime_camera) as ImageButton
        photoView = view.findViewById(R.id.crime_photo) as ImageView



        return view
    }
//Публикация новых данных
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        crimeDetailViewModel.crimeLiveData.observe(
            viewLifecycleOwner,
            Observer { crime ->
                crime?.let {
                    this.crime = crime
                    photoFile = crimeDetailViewModel.getPhotoFile(crime)
                    photoUri = FileProvider.getUriForFile(requireContext(),
                        "com.bignerbranch.android.criminal_intent.fileprovider",
                    photoFile)
                    updateUI()
                }
            }
        )
    }

    override fun onStart() {
        super.onStart()
        val titleWatcher = object : TextWatcher {
            override fun beforeTextChanged(sequence: CharSequence?, start: Int, count: Int, after: Int) {
            }
            override fun onTextChanged(sequence: CharSequence?, start: Int, count: Int, after: Int) {
                crime.title = sequence.toString()
            }
            override fun afterTextChanged(sequence: Editable?) {
            }
        }
        titleField.addTextChangedListener(titleWatcher)

        solvedCheckBox.apply {
            setOnCheckedChangeListener { _,isChecked -> crime.isSolved = isChecked }
        }
        //Нужно key к бандл ввывести в отдельные костанты
        dateButton.setOnClickListener {
            childFragmentManager.setFragmentResultListener("requestKey", viewLifecycleOwner){
                _,bundle ->
                val result = bundle.getSerializable("bundleKey") as Date

                crime.date = result
                updateUI()
            }
            //updateUI()
            DatePickerFragment.newInstance(crime.date).apply {
                show(this@CrimeFragment.childFragmentManager, DIALOG_DATE)
            }
        }
        reportButton.setOnClickListener{
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, getCrimeReport())
                putExtra(Intent.EXTRA_SUBJECT,
                    getString(R.string.crime_report_subject))
            }.also { intent ->
                startActivity(intent) }
        }
        suspectButton.apply {
            //val picContactIntent = Intent (Intent.ACTION_PICK, ContactsContract.AUTHORITY_URI)
            setOnClickListener{
                selectSuspect.launch(null)
            }
        }
        photoButton.apply {
            setOnClickListener {
                takePicture.launch(photoUri)
            }

        }


    }


    override fun onStop() {
        super.onStop()
        crimeDetailViewModel.saveCrime(crime)
    }

    private fun updateUI(){
        titleField.setText(crime.title)
        dateButton.text = crime.date.toString()
        solvedCheckBox.apply {
            isChecked = crime.isSolved
            jumpDrawablesToCurrentState()
        }
        suspectButton.text = crime.suspect.ifEmpty {
            getString(R.string.crime_suspect_text)
        }
        updatePhotoView()
    }
    private fun updatePhotoView() {
        if (photoFile.exists()){
            photoView.doOnLayout {
                val scaledDitMap = getScaledBitmap(photoFile.path, it.width, it.height)
                photoView.setImageBitmap(scaledDitMap)
                photoView.contentDescription = getString(R.string.crime_photo_image_description)
            }
        }
    }
    private fun getCrimeReport(): String {
        val solvedString = if (crime.isSolved){
            getString(R.string.crime_report_solved)
        } else {getString(R.string.crime_report_unsolved)}
        val dateString = DateFormat.format(DATE_FORMAT, crime.date).toString()
        var suspect = if (crime.suspect.isBlank()){
            getString(R.string.crime_report_no_suspect)
        } else {getString(R.string.crime_report_suspect, crime.suspect) }
        return getString(R.string.crime_report, crime.title, dateString, solvedString, suspect)

    }

    private fun pickContact (contactUri: Uri) {
        val queryField = arrayOf(ContactsContract.Contacts.DISPLAY_NAME)
        val cursor = contactUri.let {
            requireActivity().contentResolver.query(it, queryField, null, null,null)
        }
        cursor?.use {
            if (it.count > 0){
                it.moveToFirst()
                val suspect = it.getString(0)
                crime.suspect = suspect
                crimeDetailViewModel.saveCrime(crime)
                suspectButton.text = suspect
            }
        }
    }


    private fun getScaledBitmap(path: String, destWidth: Int, destHeight: Int):Bitmap{

        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(path,options)

        val srcWidth = options.outWidth.toFloat()
        val srcHeight = options.outHeight.toFloat()

        val simpleSize = if (srcHeight <= destHeight && srcWidth <= destWidth){
            1
        }else{
            val heightScale = srcHeight / destHeight
            val widthScale = srcWidth / destWidth

            minOf(heightScale,widthScale).roundToInt()
        }


        return BitmapFactory.decodeFile(path, BitmapFactory.Options().apply
        { inSampleSize = simpleSize })
    }

    companion object {
        fun newInstance(crimeId: UUID): CrimeFragment {
            val args = Bundle().apply {
                putSerializable(ARG_CRIME_ID, crimeId)
            }
            return CrimeFragment().apply {
                arguments = args
            }
        }
    }
}

